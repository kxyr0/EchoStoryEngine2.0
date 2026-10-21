package io.github.kxyr0.echotime.core.state;

import io.github.kxyr0.echotime.api.identity.TimePauseLease;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.core.mutation.TimeMutationReceipt;
import java.util.List;
import java.util.Objects;

public record EchoTimeState(
        int schemaVersion,
        EchoInstant instant,
        TimeRate rate,
        long fixedPointRemainder,
        boolean configuredRunning,
        long configRevision,
        List<TimePauseLease> pauseLeases,
        List<TimeMutationReceipt> mutationReceipts,
        List<TimeWait> waits) {
    public EchoTimeState {
        if (schemaVersion <= 0) throw new IllegalArgumentException("schemaVersion must be positive.");
        instant = Objects.requireNonNull(instant, "instant");
        rate = Objects.requireNonNull(rate, "rate");
        if (fixedPointRemainder < 0L || fixedPointRemainder >= rate.simulationTicks()) {
            throw new IllegalArgumentException("fixedPointRemainder is outside the rate denominator.");
        }
        if (configRevision < 0L) throw new IllegalArgumentException("configRevision cannot be negative.");
        pauseLeases = List.copyOf(Objects.requireNonNull(pauseLeases, "pauseLeases"));
        mutationReceipts = List.copyOf(Objects.requireNonNull(mutationReceipts, "mutationReceipts"));
        waits = List.copyOf(Objects.requireNonNull(waits, "waits"));
    }
}
