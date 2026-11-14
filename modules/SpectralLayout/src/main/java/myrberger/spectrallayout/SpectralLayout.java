package myrberger.spectrallayout;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import myrberger.spectrallayout.math.GraphMatrix;
import myrberger.spectrallayout.math.LaplacianType;
import myrberger.spectrallayout.math.SpectralEmbedding;
import myrberger.spectrallayout.preview.AxisGeometry;
import myrberger.spectrallayout.preview.AxisRegistry;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.openide.util.NbBundle;

/**
 * Spectral layout: node coordinates are eigenvectors of the graph Laplacian.
 *
 * <p>One run is one pass: {@code goAlgo()} reads the visible graph (under the
 * read lock, released at once), solves the eigenproblem without any lock and
 * writes the positions (read lock again), then {@code canAlgo()} turns false.</p>
 */
public final class SpectralLayout implements Layout {

    private static final Logger LOG = Logger.getLogger(SpectralLayout.class.getName());

    static final int DEF_X = 2;
    static final int DEF_Y = 3;
    static final String LINEAR = "linear";
    static final String LOG_SCALE = "log";
    static final double DEF_SCALE = 1000.0;
    static final double DEF_LINEAR_ZONE = 0.01;
    static final int DEF_BINS = 100;
    static final double DEF_SPACING = 1.0;
    static final int MAX_INDEX = 10000;
    static final int DEF_SAVE_COUNT = 5;
    static final int MAX_SAVE_COUNT = 100;

    private final LayoutBuilder builder;
    private GraphModel graphModel;
    private volatile boolean stopRequested;
    private volatile boolean done;

    private Integer xEigenvector = DEF_X;
    private Integer yEigenvector = DEF_Y;
    private String xScale = LINEAR;
    private String yScale = LINEAR;
    private Boolean showXAxis = Boolean.TRUE;
    private Boolean showYAxis = Boolean.TRUE;
    private Boolean flipX = Boolean.FALSE;
    private Boolean flipY = Boolean.FALSE;
    private String laplacian = LaplacianType.COMBINATORIAL.id();
    private Boolean useWeights = Boolean.TRUE;
    private Integer seed = 1;
    private Double scale = DEF_SCALE;
    private Double logLinearZone = DEF_LINEAR_ZONE;
    private Integer stackBins = DEF_BINS;
    private Double stackSpacing = DEF_SPACING;
    private Boolean stackCentered = Boolean.FALSE;
    private Integer saveCount = DEF_SAVE_COUNT;

    /** Identifies this layout for the "save eigenvectors" button (see SaveButtonEditor). */
    private final String saveToken = UUID.randomUUID().toString();
    private static final Map<String, WeakReference<SpectralLayout>> BY_TOKEN = new HashMap<>();

    public SpectralLayout(LayoutBuilder builder) {
        this.builder = builder;
        synchronized (BY_TOKEN) {
            BY_TOKEN.put(saveToken, new WeakReference<>(this));
        }
    }

    static SpectralLayout findByToken(String token) {
        synchronized (BY_TOKEN) {
            WeakReference<SpectralLayout> ref = BY_TOKEN.get(token);
            return ref == null ? null : ref.get();
        }
    }

    /**
     * Computes the eigenvectors lambda_1 .. lambda_K of the visible graph with the current
     * Laplacian settings and writes them to node columns. Called by the Save button.
     */
    EigenvectorSaver.Summary saveEigenvectors() {
        return EigenvectorSaver.save(graphModel, LaplacianType.fromId(laplacian), useWeights, seed, saveCount);
    }

    // ------------------------------------------------------------ Layout SPI

    @Override
    public void setGraphModel(GraphModel graphModel) {
        this.graphModel = graphModel;
    }

    @Override
    public void initAlgo() {
        stopRequested = false;
        done = false;
    }

    @Override
    public boolean canAlgo() {
        return !done && !stopRequested && graphModel != null;
    }

    @Override
    public void goAlgo() {
        try {
            run();
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Spectral layout failed", ex);
        } finally {
            done = true;
        }
    }

    @Override
    public void endAlgo() {
        stopRequested = true;
    }

    private void run() {
        if (graphModel == null) {
            return;
        }
        Graph graph = graphModel.getGraphVisible();

        // 1. copy what is needed out of the graph (read lock held only while copying)
        GraphSnapshot snap = GraphSnapshot.capture(graph);
        Node[] nodes = snap.nodes;
        if (stopRequested || nodes.length == 0) {
            return;
        }

        // 2. compute without a lock
        SpectralEmbedding.Config cfg = config();
        GraphMatrix matrix = GraphMatrix.build(nodes.length, snap.src, snap.dst, snap.weight, cfg.useWeights);
        long t0 = System.nanoTime();
        SpectralEmbedding.Result res = SpectralEmbedding.compute(matrix, cfg, () -> stopRequested);
        if (res.noOp || stopRequested) {
            LOG.log(Level.INFO, "Spectral layout made no changes (both axes none, empty or stopped)");
            return;
        }
        LOG.log(Level.INFO, "Spectral layout: {0} nodes, {1} method, {2} ms, lambda used x={3} y={4}",
                new Object[] {nodes.length, res.method, (System.nanoTime() - t0) / 1_000_000, res.xUsed, res.yUsed});
        logSpectrum(res);

        // 3. write positions
        graph.readLock();
        try {
            for (int i = 0; i < nodes.length; i++) {
                Node node = nodes[i];
                if (node.isFixed() || !graph.contains(node)) {
                    continue;
                }
                node.setX(finite(res.x[i]));
                node.setY(finite(res.y[i]));
            }
        } finally {
            graph.readUnlock();
        }
        AxisRegistry.set(graphModel, AxisGeometry.fromExtents(res.hasXAxis, res.hasYAxis,
                showXAxis, showYAxis, res.minX, res.maxX, res.minY, res.maxY, cfg.scale,
                axisLabel(res.xUsed, res, cfg.xLog), axisLabel(res.yUsed, res, cfg.yLog)));
    }

    /** Text such as "λ2 = 0.697" or "λ3 = 1.38 (log)"; empty for an axis without eigenvector. */
    static String axisLabel(int index, SpectralEmbedding.Result res, boolean log) {
        if (index < 1 || index > res.eigenvalues.length) {
            return "";
        }
        return String.format(java.util.Locale.ROOT, "\u03bb%d = %.4g%s", index, res.eigenvalues[index - 1],
                log ? " (log)" : "");
    }

    private static float finite(double v) {
        float f = (float) v;
        return Float.isNaN(f) || Float.isInfinite(f) ? 0f : f;
    }

    private static void logSpectrum(SpectralEmbedding.Result res) {
        if (!LOG.isLoggable(Level.INFO)) {
            return;
        }
        StringBuilder sb = new StringBuilder("Spectral layout eigenvalues:");
        for (int i = 0; i < res.eigenvalues.length && i < 12; i++) {
            sb.append(String.format(java.util.Locale.ROOT, " l%d=%.6g", i + 1, res.eigenvalues[i]));
        }
        if (!res.converged) {
            sb.append(" (iterative solver did not fully converge)");
        }
        LOG.info(sb.toString());
    }

    private SpectralEmbedding.Config config() {
        SpectralEmbedding.Config c = new SpectralEmbedding.Config();
        c.xIndex = xEigenvector;
        c.yIndex = yEigenvector;
        c.xLog = LOG_SCALE.equals(xScale);
        c.yLog = LOG_SCALE.equals(yScale);
        c.flipX = flipX;
        c.flipY = flipY;
        c.logLinearFraction = logLinearZone;
        c.scale = scale;
        c.laplacian = LaplacianType.fromId(laplacian);
        c.useWeights = useWeights;
        c.seed = seed;
        c.stackBins = stackBins;
        c.stackSpacing = stackSpacing;
        c.stackCentered = stackCentered;
        return c;
    }

    @Override
    public LayoutBuilder getBuilder() {
        return builder;
    }

    @Override
    public void resetPropertiesValues() {
        xEigenvector = DEF_X;
        yEigenvector = DEF_Y;
        xScale = LINEAR;
        yScale = LINEAR;
        showXAxis = Boolean.TRUE;
        showYAxis = Boolean.TRUE;
        flipX = Boolean.FALSE;
        flipY = Boolean.FALSE;
        laplacian = LaplacianType.COMBINATORIAL.id();
        useWeights = Boolean.TRUE;
        seed = 1;
        scale = DEF_SCALE;
        logLinearZone = DEF_LINEAR_ZONE;
        stackBins = DEF_BINS;
        stackSpacing = DEF_SPACING;
        stackCentered = Boolean.FALSE;
        saveCount = DEF_SAVE_COUNT;
    }

    // -------------------------------------------------------------- properties

    private static String msg(String key) {
        return NbBundle.getMessage(SpectralLayout.class, key);
    }

    @Override
    public LayoutProperty[] getProperties() {
        String axes = msg("SpectralLayout.category.axes");
        String spectrum = msg("SpectralLayout.category.spectrum");
        String scaling = msg("SpectralLayout.category.scaling");
        String stacking = msg("SpectralLayout.category.stacking");
        String save = msg("SpectralLayout.category.save");
        try {
            return new LayoutProperty[] {
                LayoutProperty.createProperty(this, Integer.class, msg("SpectralLayout.xEigenvector.name"), axes,
                "SpectralLayout.xEigenvector", msg("SpectralLayout.xEigenvector.desc"),
                "getXEigenvector", "setXEigenvector"),
                LayoutProperty.createProperty(this, String.class, msg("SpectralLayout.xScale.name"), axes,
                "SpectralLayout.xScale", msg("SpectralLayout.xScale.desc"),
                "getXScale", "setXScale", TagEditor.ScaleEditor.class),
                LayoutProperty.createProperty(this, Boolean.class, msg("SpectralLayout.showXAxis.name"), axes,
                "SpectralLayout.showXAxis", msg("SpectralLayout.showXAxis.desc"),
                "getShowXAxis", "setShowXAxis"),
                LayoutProperty.createProperty(this, Boolean.class, msg("SpectralLayout.flipX.name"), axes,
                "SpectralLayout.flipX", msg("SpectralLayout.flipX.desc"),
                "getFlipX", "setFlipX"),
                LayoutProperty.createProperty(this, Integer.class, msg("SpectralLayout.yEigenvector.name"), axes,
                "SpectralLayout.yEigenvector", msg("SpectralLayout.yEigenvector.desc"),
                "getYEigenvector", "setYEigenvector"),
                LayoutProperty.createProperty(this, String.class, msg("SpectralLayout.yScale.name"), axes,
                "SpectralLayout.yScale", msg("SpectralLayout.yScale.desc"),
                "getYScale", "setYScale", TagEditor.ScaleEditor.class),
                LayoutProperty.createProperty(this, Boolean.class, msg("SpectralLayout.showYAxis.name"), axes,
                "SpectralLayout.showYAxis", msg("SpectralLayout.showYAxis.desc"),
                "getShowYAxis", "setShowYAxis"),
                LayoutProperty.createProperty(this, Boolean.class, msg("SpectralLayout.flipY.name"), axes,
                "SpectralLayout.flipY", msg("SpectralLayout.flipY.desc"),
                "getFlipY", "setFlipY"),
                LayoutProperty.createProperty(this, String.class, msg("SpectralLayout.laplacian.name"), spectrum,
                "SpectralLayout.laplacian", msg("SpectralLayout.laplacian.desc"),
                "getLaplacian", "setLaplacian", TagEditor.LaplacianEditor.class),
                LayoutProperty.createProperty(this, Boolean.class, msg("SpectralLayout.useWeights.name"), spectrum,
                "SpectralLayout.useWeights", msg("SpectralLayout.useWeights.desc"),
                "getUseWeights", "setUseWeights"),
                LayoutProperty.createProperty(this, Integer.class, msg("SpectralLayout.seed.name"), spectrum,
                "SpectralLayout.seed", msg("SpectralLayout.seed.desc"),
                "getSeed", "setSeed"),
                LayoutProperty.createProperty(this, Double.class, msg("SpectralLayout.scale.name"), scaling,
                "SpectralLayout.scale", msg("SpectralLayout.scale.desc"),
                "getScale", "setScale"),
                LayoutProperty.createProperty(this, Double.class, msg("SpectralLayout.logLinear.name"), scaling,
                "SpectralLayout.logLinear", msg("SpectralLayout.logLinear.desc"),
                "getLogLinearZone", "setLogLinearZone"),
                LayoutProperty.createProperty(this, Integer.class, msg("SpectralLayout.stackBins.name"), stacking,
                "SpectralLayout.stackBins", msg("SpectralLayout.stackBins.desc"),
                "getStackBins", "setStackBins"),
                LayoutProperty.createProperty(this, Double.class, msg("SpectralLayout.stackSpacing.name"), stacking,
                "SpectralLayout.stackSpacing", msg("SpectralLayout.stackSpacing.desc"),
                "getStackSpacing", "setStackSpacing"),
                LayoutProperty.createProperty(this, Boolean.class, msg("SpectralLayout.stackCentered.name"), stacking,
                "SpectralLayout.stackCentered", msg("SpectralLayout.stackCentered.desc"),
                "getStackCentered", "setStackCentered"),
                LayoutProperty.createProperty(this, Integer.class, msg("SpectralLayout.saveCount.name"), save,
                "SpectralLayout.saveCount", msg("SpectralLayout.saveCount.desc"),
                "getSaveCount", "setSaveCount"),
                LayoutProperty.createProperty(this, String.class, msg("SpectralLayout.saveAction.name"), save,
                "SpectralLayout.saveAction", msg("SpectralLayout.saveAction.desc"),
                "getSaveAction", "setSaveAction", SaveButtonEditor.class)
            };
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Property getter/setter missing", ex);
        }
    }

    // Getters return exactly the wrapper types; setters clamp.

    public Integer getXEigenvector() {
        return xEigenvector;
    }

    public void setXEigenvector(Integer v) {
        xEigenvector = clampIndex(v, DEF_X);
    }

    public Integer getYEigenvector() {
        return yEigenvector;
    }

    public void setYEigenvector(Integer v) {
        yEigenvector = clampIndex(v, DEF_Y);
    }

    private static Integer clampIndex(Integer v, int def) {
        return v == null ? def : Math.max(0, Math.min(MAX_INDEX, v));
    }

    public String getXScale() {
        return xScale;
    }

    public void setXScale(String v) {
        xScale = LOG_SCALE.equalsIgnoreCase(v) ? LOG_SCALE : LINEAR;
    }

    public String getYScale() {
        return yScale;
    }

    public void setYScale(String v) {
        yScale = LOG_SCALE.equalsIgnoreCase(v) ? LOG_SCALE : LINEAR;
    }

    public Boolean getShowXAxis() {
        return showXAxis;
    }

    public void setShowXAxis(Boolean v) {
        showXAxis = v != null && v;
        AxisRegistry.setVisibility(graphModel, showXAxis, showYAxis);
    }

    public Boolean getShowYAxis() {
        return showYAxis;
    }

    public void setShowYAxis(Boolean v) {
        showYAxis = v != null && v;
        AxisRegistry.setVisibility(graphModel, showXAxis, showYAxis);
    }

    public Boolean getFlipX() {
        return flipX;
    }

    public void setFlipX(Boolean v) {
        flipX = v != null && v;
    }

    public Boolean getFlipY() {
        return flipY;
    }

    public void setFlipY(Boolean v) {
        flipY = v != null && v;
    }

    public String getLaplacian() {
        return laplacian;
    }

    public void setLaplacian(String v) {
        laplacian = LaplacianType.fromId(v).id();
    }

    public Boolean getUseWeights() {
        return useWeights;
    }

    public void setUseWeights(Boolean v) {
        useWeights = v == null || v;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer v) {
        seed = v == null ? 1 : v;
    }

    public Double getScale() {
        return scale;
    }

    public void setScale(Double v) {
        scale = (v == null || !(v > 0.0) || Double.isInfinite(v)) ? DEF_SCALE : Math.min(v, 1.0e7);
    }

    public Double getLogLinearZone() {
        return logLinearZone;
    }

    public void setLogLinearZone(Double v) {
        logLinearZone = (v == null || Double.isNaN(v)) ? DEF_LINEAR_ZONE : Math.max(1e-9, Math.min(1.0, v));
    }

    public Integer getStackBins() {
        return stackBins;
    }

    public void setStackBins(Integer v) {
        stackBins = v == null ? DEF_BINS : Math.max(1, Math.min(100000, v));
    }

    public Double getStackSpacing() {
        return stackSpacing;
    }

    public void setStackSpacing(Double v) {
        stackSpacing = (v == null || Double.isNaN(v)) ? DEF_SPACING : Math.max(0.0, Math.min(100.0, v));
    }

    public Boolean getStackCentered() {
        return stackCentered;
    }

    public void setStackCentered(Boolean v) {
        stackCentered = v != null && v;
    }

    public Integer getSaveCount() {
        return saveCount;
    }

    public void setSaveCount(Integer v) {
        saveCount = v == null ? DEF_SAVE_COUNT : Math.max(1, Math.min(MAX_SAVE_COUNT, v));
    }

    /** Value of the button row: the token that lets the button find this layout. */
    public String getSaveAction() {
        return saveToken;
    }

    public void setSaveAction(String ignored) {
        // the row is an action, it has no value to store
    }
}
