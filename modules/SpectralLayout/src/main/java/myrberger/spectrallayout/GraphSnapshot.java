package myrberger.spectrallayout;

import java.util.HashMap;
import java.util.Map;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;

/** Nodes and edges of the visible graph copied into plain arrays; the read lock is held only while copying. */
final class GraphSnapshot {

    final Node[] nodes;
    final int[] src;
    final int[] dst;
    final double[] weight;

    private GraphSnapshot(Node[] nodes, int[] src, int[] dst, double[] weight) {
        this.nodes = nodes;
        this.src = src;
        this.dst = dst;
        this.weight = weight;
    }

    static GraphSnapshot capture(Graph graph) {
        graph.readLock();
        try {
            Node[] nodes = graph.getNodes().toArray();
            Edge[] edges = graph.getEdges().toArray();
            Map<Node, Integer> index = new HashMap<>(nodes.length * 2);
            for (int i = 0; i < nodes.length; i++) {
                index.put(nodes[i], i);
            }
            int[] src = new int[edges.length];
            int[] dst = new int[edges.length];
            double[] weight = new double[edges.length];
            for (int e = 0; e < edges.length; e++) {
                src[e] = index.get(edges[e].getSource());
                dst[e] = index.get(edges[e].getTarget());
                weight[e] = edges[e].getWeight();
            }
            return new GraphSnapshot(nodes, src, dst, weight);
        } finally {
            graph.readUnlock();
        }
    }
}
