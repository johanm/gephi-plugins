package myrberger.spectrallayout;

import java.beans.PropertyEditorSupport;
import org.openide.util.NbBundle;

/**
 * Drop-down editor for String properties. The property value is a stable id
 * (saved with the project); the drop-down shows the localised label.
 */
abstract class TagEditor extends PropertyEditorSupport {

    private final String[] ids;
    private final String[] labelKeys;

    TagEditor(String[] ids, String[] labelKeys) {
        this.ids = ids;
        this.labelKeys = labelKeys;
    }

    private String label(int i) {
        return NbBundle.getMessage(TagEditor.class, labelKeys[i]);
    }

    @Override
    public String[] getTags() {
        String[] t = new String[ids.length];
        for (int i = 0; i < t.length; i++) {
            t[i] = label(i);
        }
        return t;
    }

    @Override
    public String getAsText() {
        Object v = getValue();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(v)) {
                return label(i);
            }
        }
        return label(0);
    }

    @Override
    public void setAsText(String text) {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(text) || label(i).equals(text)) {
                setValue(ids[i]);
                return;
            }
        }
    }

    /** Linear / Log. */
    public static final class ScaleEditor extends TagEditor {
        public ScaleEditor() {
            super(new String[] {"linear", "log"}, new String[] {"Editor.scale.linear", "Editor.scale.log"});
        }
    }

    /** Laplacian variants. */
    public static final class LaplacianEditor extends TagEditor {
        public LaplacianEditor() {
            super(new String[] {"combinatorial", "normalized", "randomwalk"},
                    new String[] {"Editor.laplacian.combinatorial", "Editor.laplacian.normalized",
                        "Editor.laplacian.randomwalk"});
        }
    }
}
