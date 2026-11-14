package myrberger.spectrallayout.math;

import org.junit.Assert;
import org.junit.Test;

public class AxisTransformTest {

    @Test
    public void signConventionMakesLargestEntryPositive() {
        double[] r = AxisTransform.signConvention(new double[] {0.1, -0.9, 0.3});
        Assert.assertArrayEquals(new double[] {-0.1, 0.9, -0.3}, r, 1e-15);
        double[] same = AxisTransform.signConvention(new double[] {0.1, 0.9, -0.3});
        Assert.assertArrayEquals(new double[] {0.1, 0.9, -0.3}, same, 1e-15);
    }

    @Test
    public void flippedInputGivesIdenticalAxisBecauseOfSignConvention() {
        double[] v = {0.2, -0.7, 0.5, 0.1};
        double[] w = {-0.2, 0.7, -0.5, -0.1};
        Assert.assertArrayEquals(AxisTransform.axisCoordinates(v, false, false, 0.01, 100),
                AxisTransform.axisCoordinates(w, false, false, 0.01, 100), 1e-12);
    }

    @Test
    public void flipReversesTheAxis() {
        double[] v = {0.2, -0.7, 0.5};
        double[] a = AxisTransform.axisCoordinates(v, false, false, 0.01, 100);
        double[] b = AxisTransform.axisCoordinates(v, true, false, 0.01, 100);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(-a[i], b[i], 1e-12);
        }
    }

    @Test
    public void linearScalingHitsHalfWidthAndKeepsRatios() {
        double[] r = AxisTransform.scaleTo(new double[] {1, -4, 2}, 100);
        Assert.assertEquals(-100.0, r[1], 1e-12);
        Assert.assertEquals(25.0, r[0], 1e-12);
        Assert.assertEquals(50.0, r[2], 1e-12);
    }

    @Test
    public void symlogIsOddMonotoneAndKeepsZero() {
        double[] v = {-100, -1, -0.01, 0, 0.01, 1, 100};
        double[] r = AxisTransform.symlog(v, 0.01);
        Assert.assertEquals(0.0, r[3], 0.0);
        for (int i = 1; i < r.length; i++) {
            Assert.assertTrue(r[i] > r[i - 1]);
        }
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(-r[i], r[6 - i], 1e-12);
        }
    }

    @Test
    public void symlogCompressesLargeRange() {
        // values over four decades become comparable steps, linear would crush the small ones
        double[] v = {0.0001, 0.001, 0.01, 0.1, 1.0};
        double[] lin = AxisTransform.axisCoordinates(v, false, false, 0.0001, 1000);
        double[] log = AxisTransform.axisCoordinates(v, false, true, 0.0001, 1000);
        Assert.assertTrue(lin[1] < 2.0);
        Assert.assertTrue(log[1] > 100.0);
    }

    @Test
    public void allZeroAndConstantInputsStayFinite() {
        for (boolean log : new boolean[] {false, true}) {
            double[] r = AxisTransform.axisCoordinates(new double[] {0, 0, 0}, false, log, 0.01, 100);
            for (double x : r) {
                Assert.assertEquals(0.0, x, 0.0);
            }
        }
        double[] c = AxisTransform.axisCoordinates(new double[] {0.5, 0.5, 0.5}, false, false, 0.01, 100);
        for (double x : c) {
            Assert.assertEquals(100.0, x, 1e-12);
        }
    }
}
