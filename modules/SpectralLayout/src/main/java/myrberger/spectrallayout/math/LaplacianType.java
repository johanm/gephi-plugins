package myrberger.spectrallayout.math;

/** Which Laplacian the eigenvectors are taken from. */
public enum LaplacianType {
    /** L = D - W. Eigenvectors used as they are. */
    COMBINATORIAL("combinatorial"),
    /** L_n = D^-1/2 L D^-1/2. Eigenvectors of L_n used as they are. */
    NORMALIZED("normalized"),
    /** Random-walk Laplacian D^-1 L: eigenvectors are D^-1/2 times those of L_n. */
    RANDOM_WALK("randomwalk");

    private final String id;

    LaplacianType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static LaplacianType fromId(String id) {
        for (LaplacianType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return COMBINATORIAL;
    }
}
