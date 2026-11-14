package myrberger.spectrallayout.math;

import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class DenseEigenTest {

    @Test
    public void residualsAndOrthonormalityOnRandomSymmetricMatrix() {
        int n = 60;
        Random rnd = new Random(7);
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double v = rnd.nextGaussian();
                a[i][j] = v;
                a[j][i] = v;
            }
        }
        DenseEigen.Result r = DenseEigen.symmetric(a);
        for (int j = 0; j < n; j++) {
            double[] v = r.vector(j);
            double norm = 0;
            for (double x : v) {
                norm += x * x;
            }
            Assert.assertEquals(1.0, norm, 1e-10);
            for (int i = 0; i < n; i++) {
                double av = 0;
                for (int k = 0; k < n; k++) {
                    av += a[i][k] * v[k];
                }
                Assert.assertEquals(r.values[j] * v[i], av, 1e-9);
            }
            if (j > 0) {
                Assert.assertTrue(r.values[j] >= r.values[j - 1]);
            }
        }
    }

    @Test
    public void oneByOneAndEmpty() {
        DenseEigen.Result r = DenseEigen.symmetric(new double[][] {{3.5}});
        Assert.assertEquals(3.5, r.values[0], 1e-14);
        Assert.assertEquals(1.0, Math.abs(r.vectors[0][0]), 1e-14);
        Assert.assertEquals(0, DenseEigen.symmetric(new double[0][0]).values.length);
    }
}
