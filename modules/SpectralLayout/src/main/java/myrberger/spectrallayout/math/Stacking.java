package myrberger.spectrallayout.math;

import java.util.Arrays;

/**
 * Distribution view for a single axis: nodes keep their exact position along
 * the axis and are stacked in the other direction, like a dot plot / histogram.
 */
public final class Stacking {

    private Stacking() {
    }

    /**
     * @param a             positions along the axis
     * @param bins          number of equally wide bins over [min, max]
     * @param spacingFactor distance between stacked nodes as a multiple of the bin width
     * @param centered      true: stacks are centred on the axis, false: they grow away from it
     * @return offsets in the stacking direction (0 = on the axis)
     */
    public static double[] stack(double[] a, int bins, double spacingFactor, boolean centered) {
        int n = a.length;
        double[] out = new double[n];
        if (n == 0) {
            return out;
        }
        bins = Math.max(1, bins);
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for (double x : a) {
            lo = Math.min(lo, x);
            hi = Math.max(hi, x);
        }
        double range = hi - lo;
        double binWidth = range > 0 ? range / bins : 1.0;
        double spacing = Math.max(0.0, spacingFactor) * binWidth;

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        final double[] av = a;
        Arrays.sort(order, (p, q) -> {
            int c = Double.compare(av[p], av[q]);
            return c != 0 ? c : Integer.compare(p, q);
        });
        int[] bin = new int[n];
        int[] count = new int[bins];
        for (int i = 0; i < n; i++) {
            int b = range > 0 ? (int) Math.floor((a[i] - lo) / binWidth) : 0;
            b = Math.max(0, Math.min(bins - 1, b));
            bin[i] = b;
            count[b]++;
        }
        int[] seen = new int[bins];
        for (int idx = 0; idx < n; idx++) {
            int i = order[idx];
            int b = bin[i];
            int rank = seen[b]++;
            out[i] = centered ? (rank - (count[b] - 1) / 2.0) * spacing : rank * spacing;
        }
        return out;
    }
}
