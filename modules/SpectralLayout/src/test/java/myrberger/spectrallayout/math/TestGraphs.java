package myrberger.spectrallayout.math;

import java.util.ArrayList;
import java.util.List;

/** Small graph builders for the tests. */
final class TestGraphs {

    private TestGraphs() {
    }

    static GraphMatrix fromEdges(int n, int[][] edges) {
        int[] s = new int[edges.length];
        int[] t = new int[edges.length];
        for (int i = 0; i < edges.length; i++) {
            s[i] = edges[i][0];
            t[i] = edges[i][1];
        }
        return GraphMatrix.build(n, s, t, null, false);
    }

    /** Ana=0, Bo=1, Cy=2, Dev=3, Eve=4 from the essay. */
    static GraphMatrix essayFiveNodes() {
        return fromEdges(5, new int[][] {{0, 1}, {0, 2}, {1, 2}, {1, 3}, {2, 4}});
    }

    /** rows x cols grid, node id = r * cols + c. */
    static GraphMatrix grid(int rows, int cols) {
        List<int[]> e = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int id = r * cols + c;
                if (c + 1 < cols) {
                    e.add(new int[] {id, id + 1});
                }
                if (r + 1 < rows) {
                    e.add(new int[] {id, id + cols});
                }
            }
        }
        return fromEdges(rows * cols, e.toArray(new int[0][]));
    }

    static GraphMatrix path(int n) {
        int[][] e = new int[n - 1][];
        for (int i = 0; i < n - 1; i++) {
            e[i] = new int[] {i, i + 1};
        }
        return fromEdges(n, e);
    }
}
