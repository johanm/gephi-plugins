package myrberger.spectrallayout.math;

/** Names of the node columns that saved eigenvectors are written to. */
public final class ColumnNames {

    private ColumnNames() {
    }

    /**
     * Examples: {@code eigenvector_2_combinatorial}, {@code eigenvector_3_normalized},
     * {@code eigenvector_2_randomwalk}, and with unweighted edges
     * {@code eigenvector_2_combinatorial_unweighted}. The number is k of lambda_k
     * (1 = smallest eigenvalue).
     */
    public static String eigenvector(LaplacianType type, boolean weighted, int k) {
        return "eigenvector_" + k + "_" + type.id() + (weighted ? "" : "_unweighted");
    }
}
