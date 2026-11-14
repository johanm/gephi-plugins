package myrberger.spectrallayout.math;

import java.util.function.BooleanSupplier;

/**
 * Computes node coordinates from Laplacian eigenvectors according to an axis
 * configuration. No Gephi imports.
 */
public final class SpectralEmbedding {

    private SpectralEmbedding() {
    }

    /** Settings of the embedding. Index 0 on an axis means "no eigenvector". */
    public static final class Config {
        public int xIndex = 2;
        public int yIndex = 3;
        public boolean xLog = false;
        public boolean yLog = false;
        public boolean flipX = false;
        public boolean flipY = false;
        public double logLinearFraction = 0.01;
        public double scale = 1000.0;
        public LaplacianType laplacian = LaplacianType.COMBINATORIAL;
        public boolean useWeights = true;
        public int seed = 1;
        public int stackBins = 100;
        public double stackSpacing = 1.0;
        public boolean stackCentered = false;
    }

    /** Output coordinates (null arrays when nothing was computed) and axis extents. */
    public static final class Result {
        public double[] x;
        public double[] y;
        /** True if no coordinates were produced (both axes "none", empty graph, cancelled). */
        public boolean noOp;
        public boolean hasXAxis;
        public boolean hasYAxis;
        public double minX, maxX, minY, maxY;
        public double[] eigenvalues = new double[0];
        public int xUsed;
        public int yUsed;
        public boolean converged = true;
        public String method = "none";
    }

    public static Result compute(GraphMatrix g, Config cfg, BooleanSupplier cancelled) {
        Result res = new Result();
        int n = g.n;
        int xi = Math.max(0, cfg.xIndex);
        int yi = Math.max(0, cfg.yIndex);
        if (n == 0 || (xi == 0 && yi == 0)) {
            res.noOp = true;
            return res;
        }
        xi = Math.min(xi, n);
        yi = Math.min(yi, n);
        int need = Math.max(xi, yi);
        SpectralSolver.Spectrum sp = SpectralSolver.solve(g, cfg.laplacian, need, cfg.seed, cancelled);
        if (cancelled != null && cancelled.getAsBoolean()) {
            res.noOp = true;
            return res;
        }
        res.eigenvalues = sp.values;
        res.converged = sp.converged;
        res.method = sp.method;
        res.xUsed = xi;
        res.yUsed = yi;

        double[] xs = null;
        double[] ys = null;
        if (xi > 0 && xi <= sp.vectors.length) {
            xs = AxisTransform.axisCoordinates(sp.vectors[xi - 1], cfg.flipX, cfg.xLog,
                    cfg.logLinearFraction, cfg.scale);
        }
        if (yi > 0 && yi <= sp.vectors.length) {
            ys = AxisTransform.axisCoordinates(sp.vectors[yi - 1], cfg.flipY, cfg.yLog,
                    cfg.logLinearFraction, cfg.scale);
        }
        if (xs == null && ys == null) {
            res.noOp = true;
            return res;
        }
        if (xs != null && ys == null) {
            ys = Stacking.stack(xs, cfg.stackBins, cfg.stackSpacing, cfg.stackCentered);
        } else if (xs == null) {
            xs = Stacking.stack(ys, cfg.stackBins, cfg.stackSpacing, cfg.stackCentered);
        }
        res.hasXAxis = xi > 0;
        res.hasYAxis = yi > 0;
        res.x = xs;
        res.y = ys;
        res.minX = min(xs);
        res.maxX = max(xs);
        res.minY = min(ys);
        res.maxY = max(ys);
        return res;
    }

    private static double min(double[] a) {
        double m = Double.POSITIVE_INFINITY;
        for (double v : a) {
            m = Math.min(m, v);
        }
        return m;
    }

    private static double max(double[] a) {
        double m = Double.NEGATIVE_INFINITY;
        for (double v : a) {
            m = Math.max(m, v);
        }
        return m;
    }
}
