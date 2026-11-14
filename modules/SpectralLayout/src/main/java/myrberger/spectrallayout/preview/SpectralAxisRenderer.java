package myrberger.spectrallayout.preview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gephi.preview.api.CanvasSize;
import org.gephi.preview.api.G2DTarget;
import org.gephi.preview.api.Item;
import org.gephi.preview.api.PDFTarget;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.api.RenderTarget;
import org.gephi.preview.api.SVGTarget;
import org.gephi.preview.spi.ItemBuilder;
import org.gephi.preview.spi.Renderer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.w3c.dom.Element;

/**
 * Draws the spectral axis lines in Preview (screen), SVG and PDF. Position 50
 * puts it before the edge renderer (100), so the axes lie behind edges and nodes.
 */
@ServiceProvider(service = Renderer.class, position = 50)
public class SpectralAxisRenderer implements Renderer {

    private static final Logger LOG = Logger.getLogger(SpectralAxisRenderer.class.getName());

    public static final String THICKNESS = "SpectralAxis.thickness";
    public static final String COLOR = "SpectralAxis.color";
    public static final String SHOW_LABELS = "SpectralAxis.showLabels";
    public static final String FONT_SIZE = "SpectralAxis.fontSize";
    static final int DEFAULT_FONT_SIZE = 40;
    static final float DEFAULT_THICKNESS = 2f;
    static final Color DEFAULT_COLOR = new Color(0x80, 0x80, 0x80);

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.name");
    }

    @Override
    public void preProcess(PreviewModel previewModel) {
        // nothing to prepare
    }

    @Override
    public void render(Item item, RenderTarget target, PreviewProperties properties) {
        if (!AxisItem.TYPE.equals(item.getType())) {
            return;
        }
        float x1 = item.getData(AxisItem.X1);
        float y1 = item.getData(AxisItem.Y1);
        float x2 = item.getData(AxisItem.X2);
        float y2 = item.getData(AxisItem.Y2);
        float thickness = properties.getFloatValue(THICKNESS);
        Color color = properties.getColorValue(COLOR);
        if (target instanceof G2DTarget) {
            renderG2D((G2DTarget) target, x1, y1, x2, y2, thickness, color);
        } else if (target instanceof SVGTarget) {
            renderSVG((SVGTarget) target, x1, y1, x2, y2, thickness, color);
        } else if (target instanceof PDFTarget) {
            renderPDF((PDFTarget) target, x1, y1, x2, y2, thickness, color);
        }
        String label = item.getData(AxisItem.LABEL);
        if (label != null && !label.isEmpty() && properties.getBooleanValue(SHOW_LABELS)) {
            int size = Math.max(1, properties.getIntValue(FONT_SIZE));
            boolean horizontal = (Boolean) item.getData(AxisItem.HORIZONTAL);
            float gap = size * 0.3f;
            // label starts just beyond the positive end of the line
            float lx = x2 + gap;
            float baseline = horizontal ? y2 + size * 0.35f : y2 - gap;
            if (target instanceof G2DTarget) {
                labelG2D((G2DTarget) target, label, lx, baseline, size, color);
            } else if (target instanceof SVGTarget) {
                labelSVG((SVGTarget) target, label, lx, baseline, size, color);
            } else if (target instanceof PDFTarget) {
                labelPDF((PDFTarget) target, label, lx, baseline, size, color);
            }
        }
    }

    private void labelG2D(G2DTarget target, String text, float x, float baseline, int size, Color color) {
        Graphics2D g = target.getGraphics();
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size));
        g.setColor(color);
        g.drawString(text, x, baseline);
        g.setFont(oldFont);
        g.setColor(oldColor);
    }

    private void labelSVG(SVGTarget target, String text, float x, float baseline, int size, Color color) {
        Element t = target.createElement("text");
        t.setAttribute("class", "spectral-axis-label");
        t.setAttribute("x", Float.toString(x));
        t.setAttribute("y", Float.toString(baseline));
        t.setAttribute("fill", target.toHexString(color));
        t.setAttribute("font-family", "sans-serif");
        t.setAttribute("font-size", Integer.toString(size));
        t.appendChild(target.createTextNode(text));
        target.getTopElement(SVGTarget.TOP_EDGES).appendChild(t);
    }

    /** Reflective for the same reason as {@link #renderPDF}. Falls back to ASCII if the font lacks the Greek letter. */
    private void labelPDF(PDFTarget target, String text, float x, float baseline, int size, Color color) {
        String[] candidates = {text, text.replace("\u03bb", "lambda")};
        for (int attempt = 0; attempt < candidates.length; attempt++) {
            try {
                Object font = PDFTarget.class.getMethod("getPDFont", Font.class)
                        .invoke(target, new Font(Font.SANS_SERIF, Font.PLAIN, size));
                Object cs = PDFTarget.class.getMethod("getContentStream").invoke(target);
                Class<?> c = cs.getClass();
                java.lang.reflect.Method setFont = null;
                for (java.lang.reflect.Method m : c.getMethods()) {
                    if (m.getName().equals("setFont") && m.getParameterCount() == 2) {
                        setFont = m;
                        break;
                    }
                }
                if (setFont == null) {
                    return;
                }
                c.getMethod("beginText").invoke(cs);
                try {
                    setFont.invoke(cs, font, (float) size);
                    c.getMethod("setNonStrokingColor", Color.class).invoke(cs, color);
                    c.getMethod("newLineAtOffset", float.class, float.class).invoke(cs, x, -baseline);
                    c.getMethod("showText", String.class).invoke(cs, candidates[attempt]);
                } finally {
                    c.getMethod("endText").invoke(cs);
                }
                return;
            } catch (ReflectiveOperationException | RuntimeException ex) {
                if (attempt == candidates.length - 1) {
                    LOG.log(Level.WARNING, "Could not write axis label to PDF", ex);
                }
            }
        }
    }

    private void renderG2D(G2DTarget target, float x1, float y1, float x2, float y2, float thickness, Color color) {
        Graphics2D g = target.getGraphics();
        Stroke oldStroke = g.getStroke();
        Color oldColor = g.getColor();
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.setColor(color);
        g.draw(new Line2D.Float(x1, y1, x2, y2));
        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    private void renderSVG(SVGTarget target, float x1, float y1, float x2, float y2, float thickness, Color color) {
        Element line = target.createElement("line");
        line.setAttribute("class", "spectral-axis");
        line.setAttribute("x1", Float.toString(x1));
        line.setAttribute("y1", Float.toString(y1));
        line.setAttribute("x2", Float.toString(x2));
        line.setAttribute("y2", Float.toString(y2));
        line.setAttribute("stroke", target.toHexString(color));
        line.setAttribute("stroke-width", Float.toString(thickness * target.getScaleRatio()));
        target.getTopElement(SVGTarget.TOP_EDGES).appendChild(line);
    }

    /**
     * PDF output goes through reflection on purpose: the content stream is a
     * pdfbox class, and referencing it directly makes the NetBeans module
     * build demand an explicit pdfbox dependency.
     */
    private void renderPDF(PDFTarget target, float x1, float y1, float x2, float y2, float thickness, Color color) {
        try {
            Object cs = PDFTarget.class.getMethod("getContentStream").invoke(target);
            Class<?> c = cs.getClass();
            c.getMethod("setStrokingColor", Color.class).invoke(cs, color);
            c.getMethod("setLineWidth", float.class).invoke(cs, thickness);
            c.getMethod("moveTo", float.class, float.class).invoke(cs, x1, -y1);
            c.getMethod("lineTo", float.class, float.class).invoke(cs, x2, -y2);
            c.getMethod("stroke").invoke(cs);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOG.log(Level.WARNING, "Could not write axis line to PDF", ex);
        }
    }

    @Override
    public void postProcess(PreviewModel previewModel, RenderTarget target, PreviewProperties properties) {
        // nothing to clean up
    }

    @Override
    public PreviewProperty[] getProperties() {
        String category = NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.category");
        return new PreviewProperty[] {
            PreviewProperty.createProperty(this, THICKNESS, Float.class,
            NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.thickness.name"),
            category,
            NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.thickness.desc"))
            .setValue(DEFAULT_THICKNESS),
            PreviewProperty.createProperty(this, COLOR, Color.class,
            NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.color.name"),
            category,
            NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.color.desc"))
            .setValue(DEFAULT_COLOR),
            PreviewProperty.createProperty(this, SHOW_LABELS, Boolean.class,
            NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.showLabels.name"),
            category,
            NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.showLabels.desc"))
            .setValue(Boolean.TRUE),
            PreviewProperty.createProperty(this, FONT_SIZE, Integer.class,
            NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.fontSize.name"),
            category,
            NbBundle.getMessage(SpectralAxisRenderer.class, "SpectralAxisRenderer.fontSize.desc"))
            .setValue(DEFAULT_FONT_SIZE)
        };
    }

    @Override
    public boolean isRendererForitem(Item item, PreviewProperties properties) {
        return AxisItem.TYPE.equals(item.getType());
    }

    @Override
    public boolean needsItemBuilder(ItemBuilder itemBuilder, PreviewProperties properties) {
        return AxisItem.TYPE.equals(itemBuilder.getType());
    }

    @Override
    public CanvasSize getCanvasSize(Item item, PreviewProperties properties) {
        float x1 = item.getData(AxisItem.X1);
        float y1 = item.getData(AxisItem.Y1);
        float x2 = item.getData(AxisItem.X2);
        float y2 = item.getData(AxisItem.Y2);
        float minX = Math.min(x1, x2);
        float minY = Math.min(y1, y2);
        float maxX = Math.max(x1, x2);
        float maxY = Math.max(y1, y2);
        String label = item.getData(AxisItem.LABEL);
        if (label != null && !label.isEmpty() && properties.getBooleanValue(SHOW_LABELS)) {
            int size = Math.max(1, properties.getIntValue(FONT_SIZE));
            float gap = size * 0.3f;
            float width = 0.62f * size * label.length();
            // generous box around the label (start at the end of the line, text above/beside it)
            minY = Math.min(minY, y2 - gap - size);
            maxY = Math.max(maxY, y2 + size);
            maxX = Math.max(maxX, x2 + gap + width);
        }
        return new CanvasSize(minX, minY, maxX - minX, maxY - minY);
    }
}
