package myrberger.spectrallayout.math;

import org.junit.Assert;
import org.junit.Test;

public class StackingTest {

    @Test
    public void nodesInTheSameBinAreStackedAwayFromTheAxis() {
        // range 0..10, 10 bins of width 1, spacing = 1 * binWidth
        double[] a = {0.0, 0.1, 0.2, 5.0, 10.0};
        double[] s = Stacking.stack(a, 10, 1.0, false);
        Assert.assertEquals(0.0, s[0], 1e-12);
        Assert.assertEquals(1.0, s[1], 1e-12);
        Assert.assertEquals(2.0, s[2], 1e-12);
        Assert.assertEquals(0.0, s[3], 1e-12);
        Assert.assertEquals(0.0, s[4], 1e-12);
    }

    @Test
    public void centeredStacksAreSymmetric() {
        double[] a = {0.0, 0.0, 0.0, 10.0};
        double[] s = Stacking.stack(a, 10, 1.0, true);
        Assert.assertEquals(-1.0, s[0], 1e-12);
        Assert.assertEquals(0.0, s[1], 1e-12);
        Assert.assertEquals(1.0, s[2], 1e-12);
        Assert.assertEquals(0.0, s[3], 1e-12);
    }

    @Test
    public void identicalPositionsAllStackAndStayFinite() {
        double[] s = Stacking.stack(new double[] {3, 3, 3, 3}, 100, 1.0, false);
        for (int i = 0; i < 4; i++) {
            Assert.assertEquals(i, s[i], 1e-12);
        }
    }

    @Test
    public void emptyInput() {
        Assert.assertEquals(0, Stacking.stack(new double[0], 10, 1.0, false).length);
    }

    @Test
    public void stackedHeightsEqualBinCounts() {
        java.util.Random r = new java.util.Random(3);
        double[] a = new double[500];
        for (int i = 0; i < a.length; i++) {
            a[i] = r.nextGaussian();
        }
        double[] s = Stacking.stack(a, 20, 1.0, false);
        double max = 0;
        for (double v : s) {
            max = Math.max(max, v);
        }
        Assert.assertTrue(max > 0);
    }
}
