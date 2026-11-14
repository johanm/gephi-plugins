package myrberger.spectrallayout.math;

import org.junit.Assert;
import org.junit.Test;

public class SpectralSolverTest {

    @Test
    public void essayFiveNodeSpectrumAndFiedlerVector() {
        GraphMatrix g = TestGraphs.essayFiveNodes();
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 5, 1, null);
        double[] expected = {0.0, 0.697, 1.382, 3.618, 4.303};
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(expected[i], s.values[i], 1e-3);
        }
        // Fiedler vector from the essay (Ana, Bo, Cy, Dev, Eve), up to sign
        double[] fiedler = {0.000, -0.205, 0.205, -0.677, 0.677};
        double[] v = s.vectors[1];
        double sign = Math.signum(v[4] * fiedler[4]);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(fiedler[i], sign * v[i], 2e-3);
        }
    }

    @Test
    public void constantVectorIsLambdaOne() {
        GraphMatrix g = TestGraphs.essayFiveNodes();
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 1, 1, null);
        Assert.assertEquals(0.0, s.values[0], 1e-10);
        for (int i = 1; i < 5; i++) {
            Assert.assertEquals(s.vectors[0][0], s.vectors[0][i], 1e-9);
        }
    }

    @Test
    public void numberOfZeroEigenvaluesIsNumberOfComponents() {
        // two triangles + one isolated node = 3 components
        GraphMatrix g = TestGraphs.fromEdges(7, new int[][] {{0, 1}, {1, 2}, {0, 2}, {3, 4}, {4, 5}, {3, 5}});
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 7, 1, null);
        int zeros = 0;
        for (double v : s.values) {
            if (v < 1e-9) {
                zeros++;
            }
        }
        Assert.assertEquals(3, zeros);
        SpectralSolver.Spectrum sn = SpectralSolver.solve(g, LaplacianType.NORMALIZED, 7, 1, null);
        zeros = 0;
        for (double v : sn.values) {
            if (v < 1e-9) {
                zeros++;
            }
        }
        Assert.assertEquals(3, zeros);
    }

    @Test
    public void normalizedSpectrumLiesInZeroTwo() {
        GraphMatrix g = TestGraphs.grid(8, 9);
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.NORMALIZED, 72, 1, null);
        for (double v : s.values) {
            Assert.assertTrue(v >= 0.0 && v <= 2.0 + 1e-9);
        }
    }

    @Test
    public void randomWalkEigenvectorsSatisfyGeneralizedProblem() {
        // D^-1 L u = lambda u  <=>  L u = lambda D u
        GraphMatrix g = TestGraphs.essayFiveNodes();
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.RANDOM_WALK, 5, 1, null);
        for (int j = 0; j < 5; j++) {
            double[] u = s.vectors[j];
            for (int i = 0; i < 5; i++) {
                double lu = g.degree[i] * u[i];
                for (int p = g.rowPtr[i]; p < g.rowPtr[i + 1]; p++) {
                    lu -= g.val[p] * u[g.col[p]];
                }
                Assert.assertEquals(s.values[j] * g.degree[i] * u[i], lu, 1e-9);
            }
        }
    }

    @Test
    public void weightsAndDirectionAndSelfLoopsAreHandled() {
        // directed pair of edges adds up to weight 5; self-loop and negative weight are ignored
        GraphMatrix g = GraphMatrix.build(3, new int[] {0, 1, 1, 0, 2}, new int[] {1, 0, 1, 2, 0},
                new double[] {2, 3, 7, -4, 0}, true);
        Assert.assertEquals(5.0, g.degree[0], 1e-12);
        Assert.assertEquals(5.0, g.degree[1], 1e-12);
        Assert.assertEquals(0.0, g.degree[2], 1e-12);
        GraphMatrix u = GraphMatrix.build(3, new int[] {0, 1}, new int[] {1, 0}, new double[] {2, 3}, false);
        Assert.assertEquals(2.0, u.degree[0], 1e-12);
    }

    @Test
    public void lobpcgMatchesAnalyticGridSpectrum() {
        // 30 x 60 grid: n = 1800 > DENSE_LIMIT, so the iterative solver runs.
        int rows = 30;
        int cols = 60;
        GraphMatrix g = TestGraphs.grid(rows, cols);
        Assert.assertTrue(g.n > SpectralSolver.DENSE_LIMIT);
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 5, 1, null);
        Assert.assertEquals("lobpcg", s.method);
        // eigenvalues of the grid: (2 - 2 cos(pi a / rows)) + (2 - 2 cos(pi b / cols))
        java.util.List<Double> all = new java.util.ArrayList<>();
        for (int a = 0; a < rows; a++) {
            for (int b = 0; b < cols; b++) {
                all.add((2 - 2 * Math.cos(Math.PI * a / rows)) + (2 - 2 * Math.cos(Math.PI * b / cols)));
            }
        }
        java.util.Collections.sort(all);
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(all.get(i), s.values[i], 1e-5);
        }
        // residual of the Fiedler vector
        double[] v = s.vectors[1];
        double[] lv = new double[g.n];
        for (int i = 0; i < g.n; i++) {
            double x = g.degree[i] * v[i];
            for (int p = g.rowPtr[i]; p < g.rowPtr[i + 1]; p++) {
                x -= g.val[p] * v[g.col[p]];
            }
            lv[i] = x - s.values[1] * v[i];
        }
        double r = 0;
        for (double x : lv) {
            r += x * x;
        }
        Assert.assertTrue("residual " + Math.sqrt(r), Math.sqrt(r) < 1e-4);
    }

    @Test
    public void lobpcgAndDenseAgreeOnNormalizedLaplacian() {
        GraphMatrix g = TestGraphs.grid(25, 41); // n = 1025 -> iterative
        SpectralSolver.Spectrum it = SpectralSolver.solve(g, LaplacianType.NORMALIZED, 4, 3, null);
        Assert.assertEquals("lobpcg", it.method);
        // dense reference by computing the full matrix directly
        double[][] a = g.denseAdjacency();
        double[][] m = new double[g.n][g.n];
        for (int i = 0; i < g.n; i++) {
            for (int j = 0; j < g.n; j++) {
                m[i][j] = -a[i][j] / Math.sqrt(g.degree[i] * g.degree[j]);
            }
            m[i][i] += 1.0;
        }
        DenseEigen.Result ref = DenseEigen.symmetric(m);
        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(ref.values[i], it.values[i], 1e-5);
        }
    }

    @Test
    public void cancellationStopsTheIterativeSolver() {
        GraphMatrix g = TestGraphs.path(1500);
        final int[] calls = {0};
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 3, 1, () -> ++calls[0] > 2);
        Assert.assertFalse(s.converged);
        Assert.assertTrue(calls[0] < 10);
    }

    @Test
    public void sameSeedGivesSameResult() {
        GraphMatrix g = TestGraphs.grid(30, 60);
        double[] a = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 3, 5, null).vectors[1];
        double[] b = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 3, 5, null).vectors[1];
        Assert.assertArrayEquals(a, b, 0.0);
    }
}
