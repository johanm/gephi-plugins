package myrberger.spectrallayout.math;

/**
 * Turns one eigenvector into final axis coordinates: sign convention, optional
 * flip, optional symmetric-log scaling, and scaling to a fixed half-width.
 */
public final class AxisTransform {

    private AxisTransform() {
    }

    /**
     * Eigenvectors are only defined up to sign. Convention: the entry with the
     * largest absolute value (first one on ties) is made positive.
     */
    public static double[] signConvention(double[] v) {
        int best = -1;
        double bestAbs = -1.0;
        for (int i = 0; i < v.length; i++) {
            double a = Math.abs(v[i]);
            if (a > bestAbs + 1e-12 * Math.max(1.0, bestAbs)) {
                bestAbs = a;
                best = i;
            }
        }
        double[] r = v.clone();
        if (best >= 0 && v[best] < 0) {
            for (int i = 0; i < r.length; i++) {
                r[i] = -r[i];
            }
        }
        return r;
    }

    /**
     * Symmetric logarithm: sign(x) * ln(1 + |x| / c). Odd, monotone, linear
     * near zero (|x| well below c) and logarithmic for |x| well above c, so
     * negative values and exact zeros are handled. c = linearFraction * max|x|.
     */
    public static double[] symlog(double[] v, double linearFraction) {
        double max = 0.0;
        for (double x : v) {
            max = Math.max(max, Math.abs(x));
        }
        double[] r = new double[v.length];
        if (!(max > 0.0)) {
            return r;
        }
        double c = Math.max(1e-9, Math.min(1.0, linearFraction)) * max;
        for (int i = 0; i < v.length; i++) {
            double a = Math.abs(v[i]);
            double y = Math.log1p(a / c);
            r[i] = v[i] < 0 ? -y : y;
        }
        return r;
    }

    /** Scales so that the largest absolute value equals {@code halfWidth}. */
    public static double[] scaleTo(double[] v, double halfWidth) {
        double max = 0.0;
        for (double x : v) {
            max = Math.max(max, Math.abs(x));
        }
        double[] r = new double[v.length];
        if (!(max > 0.0) || Double.isInfinite(max)) {
            return r;
        }
        double f = halfWidth / max;
        for (int i = 0; i < v.length; i++) {
            double y = v[i] * f;
            r[i] = Double.isFinite(y) ? y : 0.0;
        }
        return r;
    }

    /** Full pipeline for one axis. */
    public static double[] axisCoordinates(double[] eigenvector, boolean flip, boolean log,
                                           double linearFraction, double halfWidth) {
        double[] v = signConvention(eigenvector);
        if (flip) {
            for (int i = 0; i < v.length; i++) {
                v[i] = -v[i];
            }
        }
        if (log) {
            v = symlog(v, linearFraction);
        }
        return scaleTo(v, halfWidth);
    }
}
