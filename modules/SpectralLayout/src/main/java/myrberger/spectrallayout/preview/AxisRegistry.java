package myrberger.spectrallayout.preview;

import java.util.Map;
import java.util.WeakHashMap;
import org.gephi.graph.api.GraphModel;

/**
 * Hand-over point between the layout (which knows where the axes are) and the
 * Preview item builder (which draws them). Keyed by graph model, so every
 * workspace has its own axes; entries disappear with the model.
 */
public final class AxisRegistry {

    private static final Map<GraphModel, AxisGeometry> MAP = new WeakHashMap<>();

    private AxisRegistry() {
    }

    public static synchronized void set(GraphModel model, AxisGeometry geometry) {
        if (model == null) {
            return;
        }
        if (geometry == null) {
            MAP.remove(model);
        } else {
            MAP.put(model, geometry);
        }
    }

    public static synchronized AxisGeometry get(GraphModel model) {
        return model == null ? null : MAP.get(model);
    }

    /** Changes only which lines are shown; ignored if the layout has not produced axes yet. */
    public static synchronized void setVisibility(GraphModel model, boolean showX, boolean showY) {
        AxisGeometry g = get(model);
        if (g != null) {
            MAP.put(model, g.withVisibility(showX, showY));
        }
    }
}
