package myrberger.spectrallayout.math;

import org.junit.Assert;
import org.junit.Test;

public class SpectralEmbeddingTest {

    @Test
    public void defaultsUseLambdaTwoOnXAndLambdaThreeOnY() {
        GraphMatrix g = TestGraphs.essayFiveNodes();
        SpectralEmbedding.Config cfg = new SpectralEmbedding.Config();
        SpectralEmbedding.Result r = SpectralEmbedding.compute(g, cfg, null);
        Assert.assertFalse(r.noOp);
        Assert.assertTrue(r.hasXAxis && r.hasYAxis);
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 3, 1, null);
        double[] x = AxisTransform.axisCoordinates(s.vectors[1], false, false, 0.01, cfg.scale);
        double[] y = AxisTransform.axisCoordinates(s.vectors[2], false, false, 0.01, cfg.scale);
        Assert.assertArrayEquals(x, r.x, 1e-9);
        Assert.assertArrayEquals(y, r.y, 1e-9);
        Assert.assertEquals(0.697, r.eigenvalues[1], 1e-3);
        Assert.assertEquals(1.382, r.eigenvalues[2], 1e-3);
    }

    @Test
    public void anyEigenvectorCanBeUsedOnAnyAxis() {
        GraphMatrix g = TestGraphs.essayFiveNodes();
        SpectralEmbedding.Config cfg = new SpectralEmbedding.Config();
        cfg.xIndex = 5;
        cfg.yIndex = 4;
        SpectralEmbedding.Result r = SpectralEmbedding.compute(g, cfg, null);
        SpectralSolver.Spectrum s = SpectralSolver.solve(g, LaplacianType.COMBINATORIAL, 5, 1, null);
        Assert.assertArrayEquals(AxisTransform.axisCoordinates(s.vectors[4], false, false, 0.01, cfg.scale), r.x, 1e-9);
        Assert.assertArrayEquals(AxisTransform.axisCoordinates(s.vectors[3], false, false, 0.01, cfg.scale), r.y, 1e-9);
    }

    @Test
    public void noEigenvectorOnYStacksTheDistributionOfX() {
        GraphMatrix g = TestGraphs.grid(6, 6);
        SpectralEmbedding.Config cfg = new SpectralEmbedding.Config();
        cfg.xIndex = 2;
        cfg.yIndex = 0;
        cfg.stackBins = 10;
        SpectralEmbedding.Result r = SpectralEmbedding.compute(g, cfg, null);
        Assert.assertTrue(r.hasXAxis);
        Assert.assertFalse(r.hasYAxis);
        double[] expected = Stacking.stack(r.x, 10, 1.0, false);
        Assert.assertArrayEquals(expected, r.y, 1e-12);
    }

    @Test
    public void noEigenvectorOnXStacksTheDistributionOfY() {
        GraphMatrix g = TestGraphs.grid(6, 6);
        SpectralEmbedding.Config cfg = new SpectralEmbedding.Config();
        cfg.xIndex = 0;
        cfg.yIndex = 3;
        SpectralEmbedding.Result r = SpectralEmbedding.compute(g, cfg, null);
        Assert.assertFalse(r.hasXAxis);
        Assert.assertTrue(r.hasYAxis);
        Assert.assertArrayEquals(Stacking.stack(r.y, 100, 1.0, false), r.x, 1e-12);
    }

    @Test
    public void bothAxesNoneDoesNothing() {
        SpectralEmbedding.Config cfg = new SpectralEmbedding.Config();
        cfg.xIndex = 0;
        cfg.yIndex = 0;
        Assert.assertTrue(SpectralEmbedding.compute(TestGraphs.essayFiveNodes(), cfg, null).noOp);
    }

    @Test
    public void indexAboveNodeCountIsClampedAndEmptyGraphIsNoOp() {
        SpectralEmbedding.Config cfg = new SpectralEmbedding.Config();
        cfg.xIndex = 99;
        SpectralEmbedding.Result r = SpectralEmbedding.compute(TestGraphs.essayFiveNodes(), cfg, null);
        Assert.assertFalse(r.noOp);
        Assert.assertEquals(5, r.xUsed);
        Assert.assertTrue(SpectralEmbedding.compute(GraphMatrix.build(0, new int[0], new int[0], null, false),
                new SpectralEmbedding.Config(), null).noOp);
    }

    @Test
    public void logAxisIsAppliedPerAxis() {
        GraphMatrix g = TestGraphs.grid(7, 9);
        SpectralEmbedding.Config lin = new SpectralEmbedding.Config();
        SpectralEmbedding.Config log = new SpectralEmbedding.Config();
        log.xLog = true;
        SpectralEmbedding.Result a = SpectralEmbedding.compute(g, lin, null);
        SpectralEmbedding.Result b = SpectralEmbedding.compute(g, log, null);
        Assert.assertArrayEquals(a.y, b.y, 1e-9);
        boolean differs = false;
        for (int i = 0; i < a.x.length; i++) {
            if (Math.abs(a.x[i] - b.x[i]) > 1e-6) {
                differs = true;
            }
        }
        Assert.assertTrue(differs);
    }

    @Test
    public void allCoordinatesAreFiniteOnEdgelessAndSingleNodeGraphs() {
        for (int n : new int[] {1, 2, 5}) {
            SpectralEmbedding.Result r = SpectralEmbedding.compute(
                    GraphMatrix.build(n, new int[0], new int[0], null, false), new SpectralEmbedding.Config(), null);
            Assert.assertFalse(r.noOp);
            for (int i = 0; i < n; i++) {
                Assert.assertTrue(Double.isFinite(r.x[i]) && Double.isFinite(r.y[i]));
            }
        }
    }
}
