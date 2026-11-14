package myrberger.spectrallayout.preview;

import java.util.HashMap;
import java.util.Map;
import org.gephi.preview.api.Item;

/**
 * A straight axis line, in Preview coordinates (y down). Implements the Item
 * interface directly so that only preview-api is needed as a dependency.
 */
public final class AxisItem implements Item {

    public static final String TYPE = "myrberger.spectrallayout.axis";
    public static final String X1 = "axis.x1";
    public static final String Y1 = "axis.y1";
    public static final String X2 = "axis.x2";
    public static final String Y2 = "axis.y2";
    public static final String LABEL = "axis.label";
    public static final String HORIZONTAL = "axis.horizontal";

    private final Object source;
    private final Map<String, Object> data = new HashMap<>();

    public AxisItem(Object source, float x1, float y1, float x2, float y2, String label, boolean horizontal) {
        this.source = source;
        data.put(X1, x1);
        data.put(Y1, y1);
        data.put(X2, x2);
        data.put(Y2, y2);
        data.put(LABEL, label == null ? "" : label);
        data.put(HORIZONTAL, horizontal);
    }

    @Override
    public Object getSource() {
        return source;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> D getData(String key) {
        return (D) data.get(key);
    }

    @Override
    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    @Override
    public void setData(String key, Object value) {
        data.put(key, value);
    }

    @Override
    public String[] getKeys() {
        return data.keySet().toArray(new String[0]);
    }
}
