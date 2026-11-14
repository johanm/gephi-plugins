package myrberger.spectrallayout;

import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutUI;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/** Registers the Spectral Layout in Gephi's Layout panel. */
@ServiceProvider(service = LayoutBuilder.class)
public class SpectralLayoutBuilder implements LayoutBuilder {

    private final SpectralLayoutUI ui = new SpectralLayoutUI();

    @Override
    public Layout buildLayout() {
        return new SpectralLayout(this);
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(SpectralLayoutBuilder.class, "SpectralLayoutBuilder.name");
    }

    @Override
    public LayoutUI getUI() {
        return ui;
    }

    private static class SpectralLayoutUI implements LayoutUI {

        @Override
        public String getDescription() {
            return NbBundle.getMessage(SpectralLayoutBuilder.class, "SpectralLayoutUI.description");
        }

        @Override
        public javax.swing.Icon getIcon() {
            return null;
        }

        @Override
        public javax.swing.JPanel getSimplePanel(Layout layout) {
            return null;
        }

        @Override
        public int getQualityRank() {
            return -1;
        }

        @Override
        public int getSpeedRank() {
            return -1;
        }
    }
}
