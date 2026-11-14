package myrberger.spectrallayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import myrberger.spectrallayout.math.AxisTransform;
import myrberger.spectrallayout.math.ColumnNames;
import myrberger.spectrallayout.math.GraphMatrix;
import myrberger.spectrallayout.math.LaplacianType;
import myrberger.spectrallayout.math.SpectralSolver;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.Origin;
import org.gephi.graph.api.Table;
import org.openide.util.NbBundle;

/**
 * Writes the eigenvectors lambda_1 .. lambda_k of the visible graph into node
 * columns (type Double). Values are the unit-length eigenvectors with the sign
 * convention "largest magnitude entry positive", without any axis scaling.
 * An existing Double column of the same name is overwritten.
 */
final class EigenvectorSaver {

    private EigenvectorSaver() {
    }

    static final class Summary {
        final List<String> columns = new ArrayList<>();
        final List<String> skipped = new ArrayList<>();
        double[] eigenvalues = new double[0];
        int nodes;
        int totalNodes;
        int requested;
        boolean converged = true;
        String method = "none";
        /** Set instead of the rest when nothing could be done. */
        String problem;

        String text() {
            if (problem != null) {
                return problem;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(NbBundle.getMessage(EigenvectorSaver.class, "Save.result",
                    columns.size(), nodes, method)).append('\n');
            sb.append(NbBundle.getMessage(EigenvectorSaver.class, "Save.columns")).append(' ')
                    .append(String.join(", ", columns)).append('\n');
            StringBuilder ev = new StringBuilder();
            for (int i = 0; i < eigenvalues.length; i++) {
                ev.append(String.format(Locale.ROOT, "\u03bb%d = %.5g", i + 1, eigenvalues[i]));
                if (i + 1 < eigenvalues.length) {
                    ev.append(",  ");
                }
            }
            sb.append(NbBundle.getMessage(EigenvectorSaver.class, "Save.eigenvalues")).append(' ').append(ev);
            if (requested > nodes) {
                sb.append('\n').append(NbBundle.getMessage(EigenvectorSaver.class, "Save.limited", nodes, requested));
            }
            if (totalNodes > nodes) {
                sb.append('\n').append(NbBundle.getMessage(EigenvectorSaver.class, "Save.filtered", nodes, totalNodes));
            }
            if (!skipped.isEmpty()) {
                sb.append('\n').append(NbBundle.getMessage(EigenvectorSaver.class, "Save.skipped",
                        String.join(", ", skipped)));
            }
            if (!converged) {
                sb.append('\n').append(NbBundle.getMessage(EigenvectorSaver.class, "Save.notConverged"));
            }
            sb.append("\n\n").append(NbBundle.getMessage(EigenvectorSaver.class, "Save.hint"));
            return sb.toString();
        }
    }

    static Summary save(GraphModel model, LaplacianType type, boolean weighted, int seed, int k) {
        Summary s = new Summary();
        if (model == null) {
            s.problem = NbBundle.getMessage(EigenvectorSaver.class, "Save.noModel");
            return s;
        }
        Graph graph = model.getGraphVisible();
        GraphSnapshot snap = GraphSnapshot.capture(graph);
        int n = snap.nodes.length;
        if (n == 0) {
            s.problem = NbBundle.getMessage(EigenvectorSaver.class, "Save.empty");
            return s;
        }
        GraphMatrix matrix = GraphMatrix.build(n, snap.src, snap.dst, snap.weight, weighted);
        SpectralSolver.Spectrum sp = SpectralSolver.solve(matrix, type, Math.max(1, Math.min(k, n)), seed, null);
        s.nodes = n;
        s.requested = k;
        s.totalNodes = model.getGraph().getNodeCount();
        s.eigenvalues = sp.values;
        s.converged = sp.converged;
        s.method = sp.method;

        Table table = model.getNodeTable();
        for (int j = 0; j < sp.vectors.length; j++) {
            String id = ColumnNames.eigenvector(type, weighted, j + 1);
            Column col = table.getColumn(id);
            if (col == null) {
                col = table.addColumn(id, id, Double.class, Origin.DATA, null, false);
            } else if (col.getTypeClass() != Double.class) {
                s.skipped.add(id);
                continue;
            }
            double[] v = AxisTransform.signConvention(sp.vectors[j]);
            graph.readLock();
            try {
                for (int i = 0; i < n; i++) {
                    Node node = snap.nodes[i];
                    if (graph.contains(node)) {
                        node.setAttribute(col, Double.isFinite(v[i]) ? v[i] : 0.0);
                    }
                }
            } finally {
                graph.readUnlock();
            }
            s.columns.add(id);
        }
        return s;
    }
}
