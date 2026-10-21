package io.github.kxyr0.echobackgroundnpc.core.performance;

import java.util.Arrays;

public final class BenchmarkStatistics {
    public Result summarize(long[] nanoseconds) {
        if (nanoseconds.length == 0) throw new IllegalArgumentException("No benchmark samples.");
        long[] values = nanoseconds.clone();
        Arrays.sort(values);
        double median = percentile(values, 0.50) / 1_000_000.0;
        double p95 = percentile(values, 0.95) / 1_000_000.0;
        return new Result(values.length, median, p95);
    }
    private long percentile(long[] sorted, double percentile) {
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }
    public record Result(int samples, double medianMillis, double p95Millis) { }
}
