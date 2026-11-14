package myrberger.spectrallayout.preview;

/**
 * Where the axis lines are, in graph coordinates (y up as in Gephi's Overview).
 * The x axis is the horizontal line y = 0, the y axis the vertical line x = 0.
 */
public final class AxisGeometry {

    public final boolean hasX;
    public final boolean hasY;
    public final boolean showX;
    public final boolean showY;
    /** Horizontal line: from (xFrom, 0) to (xTo, 0). */
    public final double xFrom;
    public final double xTo;
    /** Vertical line: from (0, yFrom) to (0, yTo). */
    public final double yFrom;
    public final double yTo;
    /** Text drawn at the positive end of each axis line, e.g. "lambda2 = 0.697 (log)". */
    public final String xLabel;
    public final String yLabel;

    public AxisGeometry(boolean hasX, boolean hasY, boolean showX, boolean showY,
                        double xFrom, double xTo, double yFrom, double yTo, String xLabel, String yLabel) {
        this.hasX = hasX;
        this.hasY = hasY;
        this.showX = showX;
        this.showY = showY;
        this.xFrom = xFrom;
        this.xTo = xTo;
        this.yFrom = yFrom;
        this.yTo = yTo;
        this.xLabel = xLabel == null ? "" : xLabel;
        this.yLabel = yLabel == null ? "" : yLabel;
    }

    /**
     * Builds the geometry from the extents of the node coordinates. Lines
     * always include the origin and are padded by 5 % of their length.
     */
    public static AxisGeometry fromExtents(boolean hasX, boolean hasY, boolean showX, boolean showY,
                                           double minX, double maxX, double minY, double maxY, double fallbackSpan,
                                           String xLabel, String yLabel) {
        double x0 = Math.min(minX, 0.0);
        double x1 = Math.max(maxX, 0.0);
        double y0 = Math.min(minY, 0.0);
        double y1 = Math.max(maxY, 0.0);
        double px = pad(x1 - x0, fallbackSpan);
        double py = pad(y1 - y0, fallbackSpan);
        return new AxisGeometry(hasX, hasY, showX, showY, x0 - px, x1 + px, y0 - py, y1 + py, xLabel, yLabel);
    }

    private static double pad(double span, double fallback) {
        return 0.05 * (span > 0 ? span : fallback);
    }

    public AxisGeometry withVisibility(boolean newShowX, boolean newShowY) {
        return new AxisGeometry(hasX, hasY, newShowX, newShowY, xFrom, xTo, yFrom, yTo, xLabel, yLabel);
    }
}
