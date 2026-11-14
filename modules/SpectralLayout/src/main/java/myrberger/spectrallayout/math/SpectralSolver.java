package myrberger.spectrallayout.math;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Computes the k smallest eigenpairs of a graph Laplacian.
 *
 * <ul>
 * <li>Small graphs (n up to {@link #DENSE_LIMIT}): exact, dense decomposition.</li>
 * <li>Larger graphs: block LOBPCG (locally optimal block preconditioned
 * conjugate gradient, Rayleigh-Ritz on [X, W, P]) with a Jacobi
 * preconditioner. Only sparse matrix-vector products are used.</li>
 * </ul>
 *
 * <p>Eigenvalues are ascending, so index 0 is lambda_1 (zero for a connected
 * graph; the number of zero eigenvalues is the number of components).
 * Eigenvectors inside a degenerate eigenspace are arbitrary rotations of each
 * other. No Gephi imports.</p>
 */
public final class SpectralSolver {

    private static final Logger LOG = Logger.getLogger(SpectralSolver.class.getName());

    /** Largest n solved with the dense method. */
    public static final int DENSE_LIMIT = 1000;
    private static final int MAX_ITER = 400;
    private static final double TOL = 1e-7;

    private SpectralSolver() {
    }

    /** Eigenvalues ascending; vectors[j] is the eigenvector of values[j], length n. */
    public static final class Spectrum {
        public final double[] values;
        public final double[][] vectors;
        public final boolean converged;
        public final String method;

        Spectrum(double[] values, double[][] vectors, boolean converged, String method) {
            this.values = values;
            this.vectors = vectors;
            this.converged = converged;
            this.method = method;
        }
    }

    public static Spectrum solve(GraphMatrix g, LaplacianType type, int k, long seed, BooleanSupplier cancelled) {
        int n = g.n;
        k = Math.max(0, Math.min(k, n));
        if (k == 0) {
            return new Spectrum(new double[0], new double[0][], true, "none");
        }
        if (n <= DENSE_LIMIT) {
            return dense(g, type, k);
        }
        return lobpcg(g, type, k, seed, cancelled == null ? () -> false : cancelled);
    }

    // ------------------------------------------------------------------ helpers

    /** 1/sqrt(d_i), or 0 for isolates. */
    private static double[] invSqrtDegree(GraphMatrix g) {
        double[] s = new double[g.n];
        for (int i = 0; i < g.n; i++) {
            s[i] = g.degree[i] > 0 ? 1.0 / Math.sqrt(g.degree[i]) : 0.0;
        }
        return s;
    }

    /** Back-transformation of eigenvectors of L_n to random-walk eigenvectors. */
    private static void toRandomWalk(GraphMatrix g, double[][] vectors) {
        double[] s = invSqrtDegree(g);
        for (double[] v : vectors) {
            for (int i = 0; i < g.n; i++) {
                v[i] *= (s[i] > 0 ? s[i] : 1.0);
            }
        }
    }

    // -------------------------------------------------------------------- dense

    private static Spectrum dense(GraphMatrix g, LaplacianType type, int k) {
        int n = g.n;
        double[][] a = g.denseAdjacency();
        double[][] m = new double[n][n];
        if (type == LaplacianType.COMBINATORIAL) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    m[i][j] = -a[i][j];
                }
                m[i][i] += g.degree[i];
            }
        } else {
            double[] s = invSqrtDegree(g);
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    m[i][j] = -a[i][j] * s[i] * s[j];
                }
                m[i][i] += g.degree[i] > 0 ? 1.0 : 0.0;
            }
        }
        DenseEigen.Result r = DenseEigen.symmetric(m);
        double[] values = new double[k];
        double[][] vectors = new double[k][];
        for (int j = 0; j < k; j++) {
            values[j] = Math.max(0.0, r.values[j]);
            vectors[j] = r.vector(j);
        }
        if (type == LaplacianType.RANDOM_WALK) {
            toRandomWalk(g, vectors);
        }
        return new Spectrum(values, vectors, true, "dense");
    }

    // ------------------------------------------------------------------- LOBPCG

    /** Matrix-free operator and preconditioner. */
    private static final class Op {
        final GraphMatrix g;
        final boolean combinatorial;
        final double[] invSqrt;
        final double[] tmp;

        Op(GraphMatrix g, LaplacianType type) {
            this.g = g;
            this.combinatorial = type == LaplacianType.COMBINATORIAL;
            this.invSqrt = combinatorial ? null : invSqrtDegree(g);
            this.tmp = new double[g.n];
        }

        void mult(double[] x, double[] y) {
            int n = g.n;
            if (combinatorial) {
                g.adjacencyMult(x, y);
                for (int i = 0; i < n; i++) {
                    y[i] = g.degree[i] * x[i] - y[i];
                }
            } else {
                for (int i = 0; i < n; i++) {
                    tmp[i] = x[i] * invSqrt[i];
                }
                g.adjacencyMult(tmp, y);
                for (int i = 0; i < n; i++) {
                    y[i] = (g.degree[i] > 0 ? x[i] : 0.0) - invSqrt[i] * y[i];
                }
            }
        }

        /** Jacobi preconditioner for the combinatorial Laplacian, identity otherwise. */
        void precond(double[] r, double[] w) {
            for (int i = 0; i < g.n; i++) {
                w[i] = (combinatorial && g.degree[i] > 0) ? r[i] / g.degree[i] : r[i];
            }
        }

        /** Upper bound of the largest eigenvalue (Gershgorin). */
        double normBound() {
            return combinatorial ? 2.0 * g.maxDegree() : 2.0;
        }
    }

    private static double dot(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    private static double norm(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    private static void axpy(double alpha, double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) {
            y[i] += alpha * x[i];
        }
    }

    /**
     * Orthonormalises the columns in {@code cand} against {@code basis} and each
     * other (modified Gram-Schmidt, twice). Columns that become numerically
     * dependent are dropped. Returns the kept columns.
     */
    private static double[][] orthonormalize(double[][] basis, int basisCount, double[][] cand, int candCount) {
        double[][] kept = new double[candCount][];
        int nk = 0;
        for (int c = 0; c < candCount; c++) {
            double[] v = cand[c].clone();
            double nv = norm(v);
            if (!(nv > 1e-300)) {
                continue;
            }
            for (int i = 0; i < v.length; i++) {
                v[i] /= nv;
            }
            for (int pass = 0; pass < 2; pass++) {
                for (int b = 0; b < basisCount; b++) {
                    axpy(-dot(basis[b], v), basis[b], v);
                }
                for (int b = 0; b < nk; b++) {
                    axpy(-dot(kept[b], v), kept[b], v);
                }
            }
            double nr = norm(v);
            if (nr < 1e-8) {
                continue;
            }
            for (int i = 0; i < v.length; i++) {
                v[i] /= nr;
            }
            kept[nk++] = v;
        }
        double[][] out = new double[nk][];
        System.arraycopy(kept, 0, out, 0, nk);
        return out;
    }

    private static Spectrum lobpcg(GraphMatrix g, LaplacianType type, int k, long seed, BooleanSupplier cancelled) {
        final int n = g.n;
        final int m = Math.min(n, k + Math.max(3, k / 2));
        final Op op = new Op(g, type);
        final double tolAbs = TOL * op.normBound();

        // random start, orthonormal
        Random rnd = new Random(seed);
        double[][] start = new double[m][n];
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                start[j][i] = rnd.nextGaussian();
            }
        }
        double[][] x = orthonormalize(new double[0][], 0, start, m);
        int mx = x.length;
        double[][] ax = new double[mx][n];
        for (int j = 0; j < mx; j++) {
            op.mult(x[j], ax[j]);
        }
        double[] theta = new double[mx];
        // initial Rayleigh-Ritz
        {
            double[][] t = new double[mx][mx];
            for (int a = 0; a < mx; a++) {
                for (int b = 0; b < mx; b++) {
                    t[a][b] = dot(x[a], ax[b]);
                }
            }
            symmetrize(t);
            DenseEigen.Result r = DenseEigen.symmetric(t);
            double[][][] rot = rotate(new double[][][] {x, ax}, r, mx);
            x = rot[0];
            ax = rot[1];
            System.arraycopy(r.values, 0, theta, 0, mx);
        }

        double[][] p = new double[0][];
        boolean converged = false;
        int it;
        for (it = 0; it < MAX_ITER; it++) {
            if (cancelled.getAsBoolean()) {
                break;
            }
            // residuals
            double[][] w = new double[mx][];
            int nw = 0;
            boolean allConverged = true;
            for (int j = 0; j < mx; j++) {
                double[] r = new double[n];
                for (int i = 0; i < n; i++) {
                    r[i] = ax[j][i] - theta[j] * x[j][i];
                }
                double rn = norm(r);
                if (rn > tolAbs) {
                    if (j < k) {
                        allConverged = false;
                    }
                    double[] pw = new double[n];
                    op.precond(r, pw);
                    w[nw++] = pw;
                }
            }
            if (allConverged) {
                converged = true;
                break;
            }
            // candidate directions: W then P, orthonormalised against X
            double[][] cand = new double[nw + p.length][];
            System.arraycopy(w, 0, cand, 0, nw);
            System.arraycopy(p, 0, cand, nw, p.length);
            double[][] q = orthonormalize(x, mx, cand, cand.length);
            int nq = q.length;
            if (nq == 0) {
                converged = true; // nothing left to improve
                break;
            }
            double[][] aq = new double[nq][n];
            for (int j = 0; j < nq; j++) {
                op.mult(q[j], aq[j]);
            }
            int s = mx + nq;
            double[][] basis = new double[s][];
            double[][] abasis = new double[s][];
            for (int j = 0; j < mx; j++) {
                basis[j] = x[j];
                abasis[j] = ax[j];
            }
            for (int j = 0; j < nq; j++) {
                basis[mx + j] = q[j];
                abasis[mx + j] = aq[j];
            }
            double[][] t = new double[s][s];
            for (int a = 0; a < s; a++) {
                for (int b = a; b < s; b++) {
                    double d = dot(basis[a], abasis[b]);
                    t[a][b] = d;
                    t[b][a] = d;
                }
            }
            DenseEigen.Result r = DenseEigen.symmetric(t);
            int take = mx;
            double[][] newX = new double[take][n];
            double[][] newAX = new double[take][n];
            double[][] newP = new double[take][n];
            for (int j = 0; j < take; j++) {
                for (int a = 0; a < s; a++) {
                    double c = r.vectors[a][j];
                    axpy(c, basis[a], newX[j]);
                    axpy(c, abasis[a], newAX[j]);
                    if (a >= mx) {
                        axpy(c, basis[a], newP[j]);
                    }
                }
                theta[j] = r.values[j];
            }
            x = newX;
            ax = newAX;
            p = newP;
            if ((it + 1) % 40 == 0) { // remove accumulated rounding in A*X
                x = orthonormalize(new double[0][], 0, x, mx);
                mx = x.length;
                ax = new double[mx][n];
                for (int j = 0; j < mx; j++) {
                    op.mult(x[j], ax[j]);
                }
                p = new double[0][];
            }
        }
        if (!converged && !cancelled.getAsBoolean()) {
            LOG.log(Level.INFO, "LOBPCG stopped after {0} iterations without reaching tolerance", it);
        }
        int kk = Math.min(k, mx);
        double[] values = new double[kk];
        double[][] vectors = new double[kk][];
        for (int j = 0; j < kk; j++) {
            values[j] = Math.max(0.0, theta[j]);
            vectors[j] = x[j];
        }
        if (type == LaplacianType.RANDOM_WALK) {
            toRandomWalk(g, vectors);
        }
        return new Spectrum(values, vectors, converged, "lobpcg");
    }

    private static void symmetrize(double[][] t) {
        for (int a = 0; a < t.length; a++) {
            for (int b = a + 1; b < t.length; b++) {
                double v = 0.5 * (t[a][b] + t[b][a]);
                t[a][b] = v;
                t[b][a] = v;
            }
        }
    }

    /** Returns {X C, AX C} where C holds the eigenvectors of the projected matrix. */
    private static double[][][] rotate(double[][][] sets, DenseEigen.Result r, int count) {
        double[][][] out = new double[sets.length][][];
        for (int s = 0; s < sets.length; s++) {
            int n = sets[s][0].length;
            double[][] res = new double[count][n];
            for (int j = 0; j < count; j++) {
                for (int a = 0; a < count; a++) {
                    axpy(r.vectors[a][j], sets[s][a], res[j]);
                }
            }
            out[s] = res;
        }
        return out;
    }
}
