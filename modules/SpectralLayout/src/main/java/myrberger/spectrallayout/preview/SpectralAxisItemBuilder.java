package myrberger.spectrallayout.preview;

import java.util.ArrayList;
import java.util.List;
import org.gephi.graph.api.Graph;
import org.gephi.preview.api.Item;
import org.gephi.preview.spi.ItemBuilder;
import org.openide.util.lookup.ServiceProvider;

/** Produces one {@link AxisItem} per visible axis for Preview. */
@ServiceProvider(service = ItemBuilder.class)
public class SpectralAxisItemBuilder implements ItemBuilder {

    @Override
    public Item[] getItems(Graph graph) {
        AxisGeometry g = AxisRegistry.get(graph.getModel());
        List<Item> items = new ArrayList<>(2);
        if (g != null) {
            // Preview uses screen coordinates: y points down, so graph y is negated.
            if (g.hasX && g.showX) {
                items.add(new AxisItem("x", (float) g.xFrom, 0f, (float) g.xTo, 0f, g.xLabel, true));
            }
            if (g.hasY && g.showY) {
                items.add(new AxisItem("y", 0f, (float) -g.yFrom, 0f, (float) -g.yTo, g.yLabel, false));
            }
        }
        return items.toArray(new Item[0]);
    }

    @Override
    public String getType() {
        return AxisItem.TYPE;
    }
}
