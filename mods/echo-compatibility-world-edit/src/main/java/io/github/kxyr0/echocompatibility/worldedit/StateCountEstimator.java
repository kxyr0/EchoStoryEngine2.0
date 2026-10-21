package io.github.kxyr0.echocompatibility.worldedit;

public final class StateCountEstimator {
    private StateCountEstimator() {
    }

    public static Estimate estimate(Iterable<Integer> propertySizes, long limit) {
        if (limit < 1L) {
            throw new IllegalArgumentException("limit must be positive");
        }
        long count = 1L;
        for (int size : propertySizes) {
            if (size < 0) {
                throw new IllegalArgumentException("property size must not be negative");
            }
            if (size == 0) {
                return new Estimate(0L, false);
            }
            if (count > limit / size) {
                return new Estimate(limit + 1L, true);
            }
            count *= size;
            if (count > limit) {
                return new Estimate(count, true);
            }
        }
        return new Estimate(count, false);
    }

    public record Estimate(long count, boolean exceedsLimit) {
    }
}
