package myrberger.spectrallayout.math;

/**
 * Eigen-decomposition of a dense real symmetric matrix (Householder
 * tridiagonalisation followed by implicit QL, the classic EISPACK tred2/tql2
 * scheme). Pure Java, no dependencies.
 *
 * <p>Result: eigenvalues ascending in {@code d}; eigenvector j is column j of
 * {@code V} (component i is {@code V[i][j]}), unit length.</p>
 */
public final class DenseEigen {

    private DenseEigen() {
    }

    /**
     * @param a symmetric n x n matrix (copied, not modified)
     * @return an object holding eigenvalues and eigenvectors
     */
    public static Result symmetric(double[][] a) {
        int n = a.length;
        double[][] v = new double[n][];
        for (int i = 0; i < n; i++) {
            v[i] = a[i].clone();
        }
        double[] d = new double[n];
        double[] e = new double[n];
        if (n > 0) {
            tred2(n, v, d, e);
            tql2(n, v, d, e);
        }
        return new Result(d, v);
    }

    /** Eigenvalues (ascending) and eigenvectors (columns). */
    public static final class Result {
        public final double[] values;
        public final double[][] vectors;

        Result(double[] values, double[][] vectors) {
            this.values = values;
            this.vectors = vectors;
        }

        /** Copy of eigenvector j as a plain array. */
        public double[] vector(int j) {
            double[] r = new double[values.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = vectors[i][j];
            }
            return r;
        }
    }

    private static void tred2(int n, double[][] v, double[] d, double[] e) {
        for (int j = 0; j < n; j++) {
            d[j] = v[n - 1][j];
        }
        for (int i = n - 1; i > 0; i--) {
            double scale = 0.0;
            double h = 0.0;
            for (int k = 0; k < i; k++) {
                scale += Math.abs(d[k]);
            }
            if (scale == 0.0) {
                e[i] = d[i - 1];
                for (int j = 0; j < i; j++) {
                    d[j] = v[i - 1][j];
                    v[i][j] = 0.0;
                    v[j][i] = 0.0;
                }
            } else {
                for (int k = 0; k < i; k++) {
                    d[k] /= scale;
                    h += d[k] * d[k];
                }
                double f = d[i - 1];
                double g = Math.sqrt(h);
                if (f > 0) {
                    g = -g;
                }
                e[i] = scale * g;
                h = h - f * g;
                d[i - 1] = f - g;
                for (int j = 0; j < i; j++) {
                    e[j] = 0.0;
                }
                for (int j = 0; j < i; j++) {
                    f = d[j];
                    v[j][i] = f;
                    g = e[j] + v[j][j] * f;
                    for (int k = j + 1; k <= i - 1; k++) {
                        g += v[k][j] * d[k];
                        e[k] += v[k][j] * f;
                    }
                    e[j] = g;
                }
                f = 0.0;
                for (int j = 0; j < i; j++) {
                    e[j] /= h;
                    f += e[j] * d[j];
                }
                double hh = f / (h + h);
                for (int j = 0; j < i; j++) {
                    e[j] -= hh * d[j];
                }
                for (int j = 0; j < i; j++) {
                    f = d[j];
                    g = e[j];
                    for (int k = j; k <= i - 1; k++) {
                        v[k][j] -= (f * e[k] + g * d[k]);
                    }
                    d[j] = v[i - 1][j];
                    v[i][j] = 0.0;
                }
            }
            d[i] = h;
        }
        for (int i = 0; i < n - 1; i++) {
            v[n - 1][i] = v[i][i];
            v[i][i] = 1.0;
            double h = d[i + 1];
            if (h != 0.0) {
                for (int k = 0; k <= i; k++) {
                    d[k] = v[k][i + 1] / h;
                }
                for (int j = 0; j <= i; j++) {
                    double g = 0.0;
                    for (int k = 0; k <= i; k++) {
                        g += v[k][i + 1] * v[k][j];
                    }
                    for (int k = 0; k <= i; k++) {
                        v[k][j] -= g * d[k];
                    }
                }
            }
            for (int k = 0; k <= i; k++) {
                v[k][i + 1] = 0.0;
            }
        }
        for (int j = 0; j < n; j++) {
            d[j] = v[n - 1][j];
            v[n - 1][j] = 0.0;
        }
        v[n - 1][n - 1] = 1.0;
        e[0] = 0.0;
    }

    private static void tql2(int n, double[][] v, double[] d, double[] e) {
        for (int i = 1; i < n; i++) {
            e[i - 1] = e[i];
        }
        e[n - 1] = 0.0;
        double f = 0.0;
        double tst1 = 0.0;
        double eps = Math.pow(2.0, -52.0);
        for (int l = 0; l < n; l++) {
            tst1 = Math.max(tst1, Math.abs(d[l]) + Math.abs(e[l]));
            int m = l;
            while (m < n) {
                if (Math.abs(e[m]) <= eps * tst1) {
                    break;
                }
                m++;
            }
            if (m > l) {
                int iter = 0;
                do {
                    iter++;
                    double g = d[l];
                    double p = (d[l + 1] - g) / (2.0 * e[l]);
                    double r = Math.hypot(p, 1.0);
                    if (p < 0) {
                        r = -r;
                    }
                    d[l] = e[l] / (p + r);
                    d[l + 1] = e[l] * (p + r);
                    double dl1 = d[l + 1];
                    double h = g - d[l];
                    for (int i = l + 2; i < n; i++) {
                        d[i] -= h;
                    }
                    f += h;
                    p = d[m];
                    double c = 1.0;
                    double c2 = c;
                    double c3 = c;
                    double el1 = e[l + 1];
                    double s = 0.0;
                    double s2 = 0.0;
                    for (int i = m - 1; i >= l; i--) {
                        c3 = c2;
                        c2 = c;
                        s2 = s;
                        g = c * e[i];
                        h = c * p;
                        r = Math.hypot(p, e[i]);
                        e[i + 1] = s * r;
                        s = e[i] / r;
                        c = p / r;
                        p = c * d[i] - s * g;
                        d[i + 1] = h + s * (c * g + s * d[i]);
                        for (int k = 0; k < n; k++) {
                            h = v[k][i + 1];
                            v[k][i + 1] = s * v[k][i] + c * h;
                            v[k][i] = c * v[k][i] - s * h;
                        }
                    }
                    p = -s * s2 * c3 * el1 * e[l] / dl1;
                    e[l] = s * p;
                    d[l] = c * p;
                } while (Math.abs(e[l]) > eps * tst1 && iter < 200);
            }
            d[l] = d[l] + f;
            e[l] = 0.0;
        }
        for (int i = 0; i < n - 1; i++) {
            int k = i;
            double p = d[i];
            for (int j = i + 1; j < n; j++) {
                if (d[j] < p) {
                    k = j;
                    p = d[j];
                }
            }
            if (k != i) {
                d[k] = d[i];
                d[i] = p;
                for (int j = 0; j < n; j++) {
                    p = v[j][i];
                    v[j][i] = v[j][k];
                    v[j][k] = p;
                }
            }
        }
    }
}
