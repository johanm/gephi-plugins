package myrberger.spectrallayout.math;

/**
 * Symmetric weighted adjacency of an undirected view of a graph, stored as
 * compressed rows. Edge direction is ignored (both directions add up), parallel
 * edges add up, self-loops and edges with a non-positive or non-finite weight
 * are skipped (the Laplacian is defined for non-negative weights).
 *
 * <p>No Gephi imports: the Gephi layer copies edges into plain arrays.</p>
 */
public final class GraphMatrix {

    public final int n;
    final int[] rowPtr;
    final int[] col;
    final double[] val;
    /** Weighted degree of every node. */
    public final double[] degree;

    private GraphMatrix(int n, int[] rowPtr, int[] col, double[] val, double[] degree) {
        this.n = n;
        this.rowPtr = rowPtr;
        this.col = col;
        this.val = val;
        this.degree = degree;
    }

    /**
     * @param n          number of nodes
     * @param src        edge sources (node indices 0..n-1)
     * @param dst        edge targets
     * @param weight     edge weights, may be null
     * @param useWeights if false every accepted edge counts 1
     */
    public static GraphMatrix build(int n, int[] src, int[] dst, double[] weight, boolean useWeights) {
        int m = src.length;
        boolean[] ok = new boolean[m];
        double[] w = new double[m];
        int[] cnt = new int[n + 1];
        for (int e = 0; e < m; e++) {
            int s = src[e];
            int t = dst[e];
            if (s == t || s < 0 || t < 0 || s >= n || t >= n) {
                continue;
            }
            double x = (useWeights && weight != null) ? weight[e] : 1.0;
            if (!(x > 0.0) || Double.isInfinite(x)) {
                continue;
            }
            ok[e] = true;
            w[e] = x;
            cnt[s + 1]++;
            cnt[t + 1]++;
        }
        for (int i = 0; i < n; i++) {
            cnt[i + 1] += cnt[i];
        }
        int[] rowPtr = cnt;
        int[] fill = new int[n];
        int nnz = rowPtr[n];
        int[] col = new int[nnz];
        double[] val = new double[nnz];
        double[] deg = new double[n];
        for (int e = 0; e < m; e++) {
            if (!ok[e]) {
                continue;
            }
            int s = src[e];
            int t = dst[e];
            int ps = rowPtr[s] + fill[s]++;
            col[ps] = t;
            val[ps] = w[e];
            int pt = rowPtr[t] + fill[t]++;
            col[pt] = s;
            val[pt] = w[e];
            deg[s] += w[e];
            deg[t] += w[e];
        }
        return new GraphMatrix(n, rowPtr, col, val, deg);
    }

    /** y = A x (adjacency). */
    void adjacencyMult(double[] x, double[] y) {
        for (int i = 0; i < n; i++) {
            double s = 0.0;
            for (int p = rowPtr[i]; p < rowPtr[i + 1]; p++) {
                s += val[p] * x[col[p]];
            }
            y[i] = s;
        }
    }

    /** Dense symmetric adjacency matrix (only for small n). */
    double[][] denseAdjacency() {
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int p = rowPtr[i]; p < rowPtr[i + 1]; p++) {
                a[i][col[p]] += val[p];
            }
        }
        return a;
    }

    public double maxDegree() {
        double m = 0.0;
        for (double d : degree) {
            m = Math.max(m, d);
        }
        return m;
    }
}
