package myrberger.spectrallayout;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyEditorSupport;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.openide.util.NbBundle;

/**
 * Layout settings only offer values, not buttons. This editor turns one
 * property row into a button: the row shows a short text and a "..." button
 * which opens a small panel with a Save button. The property value is just a
 * token that identifies the layout instance.
 */
public final class SaveButtonEditor extends PropertyEditorSupport {

    @Override
    public String getAsText() {
        return NbBundle.getMessage(SaveButtonEditor.class, "SaveButton.text");
    }

    @Override
    public void setAsText(String text) {
        // read-only: the row is an action, not a value
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    @Override
    public Component getCustomEditor() {
        return new SavePanel(String.valueOf(getValue()));
    }

    /** The panel behind the "..." button. */
    static final class SavePanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private final String token;
        private final JButton button;
        private final JTextArea status;

        SavePanel(String token) {
            super(new BorderLayout(8, 8));
            this.token = token;
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JLabel info = new JLabel(NbBundle.getMessage(SaveButtonEditor.class, "SavePanel.description"));
            button = new JButton(NbBundle.getMessage(SaveButtonEditor.class, "SavePanel.button"));
            status = new JTextArea(6, 50);
            status.setEditable(false);
            status.setLineWrap(true);
            status.setWrapStyleWord(true);
            add(info, BorderLayout.NORTH);
            add(new JScrollPane(status), BorderLayout.CENTER);
            JPanel south = new JPanel(new BorderLayout());
            south.add(button, BorderLayout.WEST);
            add(south, BorderLayout.SOUTH);
            setPreferredSize(new Dimension(560, 260));
            button.addActionListener(e -> start());
        }

        private void start() {
            SpectralLayout layout = SpectralLayout.findByToken(token);
            if (layout == null) {
                status.setText(NbBundle.getMessage(SaveButtonEditor.class, "Save.noLayout"));
                return;
            }
            button.setEnabled(false);
            status.setText(NbBundle.getMessage(SaveButtonEditor.class, "SavePanel.working"));
            Thread t = new Thread(() -> {
                String text;
                try {
                    text = layout.saveEigenvectors().text();
                } catch (RuntimeException ex) {
                    text = String.valueOf(ex);
                }
                final String result = text;
                SwingUtilities.invokeLater(() -> {
                    status.setText(result);
                    button.setEnabled(true);
                });
            }, "Spectral layout: save eigenvectors");
            t.setDaemon(true);
            t.start();
        }
    }
}
