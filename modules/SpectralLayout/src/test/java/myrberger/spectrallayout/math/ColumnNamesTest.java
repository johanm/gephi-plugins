package myrberger.spectrallayout.math;

import org.junit.Assert;
import org.junit.Test;

public class ColumnNamesTest {

    @Test
    public void nameContainsOrderAndMethod() {
        Assert.assertEquals("eigenvector_2_combinatorial", ColumnNames.eigenvector(LaplacianType.COMBINATORIAL, true, 2));
        Assert.assertEquals("eigenvector_3_normalized", ColumnNames.eigenvector(LaplacianType.NORMALIZED, true, 3));
        Assert.assertEquals("eigenvector_12_randomwalk", ColumnNames.eigenvector(LaplacianType.RANDOM_WALK, true, 12));
    }

    @Test
    public void unweightedRunsGetTheirOwnColumns() {
        Assert.assertEquals("eigenvector_2_combinatorial_unweighted",
                ColumnNames.eigenvector(LaplacianType.COMBINATORIAL, false, 2));
        Assert.assertTrue(!ColumnNames.eigenvector(LaplacianType.COMBINATORIAL, true, 2)
                .equals(ColumnNames.eigenvector(LaplacianType.COMBINATORIAL, false, 2)));
    }
}
