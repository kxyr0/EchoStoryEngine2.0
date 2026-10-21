package io.github.kxyr0.echotime.forge.command;

import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.forge.host.ForgeEchoTimeHost;
import java.util.Comparator;
import java.util.List;

final class EchoTimeStatusText {
    List<String> status(ForgeEchoTimeHost host) {
        EchoTimeSnapshot value = host.snapshot();
        return List.of(
                "Echo Day: " + value.instant().day().value(),
                "Time: " + value.instant().timeOfDay().formatHHmm() + ":" + value.instant().timeOfDay().second(),
                "Absolute instant: " + value.instant().absoluteMicroseconds(),
                "Running: " + value.running(),
                "Rate: " + value.effectiveRate().gameMicroseconds() + "/"
                        + value.effectiveRate().simulationTicks() + " game micros/tick",
                "Pause leases: " + host.pauseLeases().size(),
                "Clock revision: " + value.instant().revision().value(),
                "Minecraft derived dayTime: " + host.derivedVanillaDayTime(),
                "Sun dimensions: " + host.config().sunDimensions());
    }

    List<String> next(ForgeEchoTimeHost host) {
        long current = host.snapshot().instant().timeOfDay().microseconds();
        return host.scheduleEntries().stream()
                .sorted(Comparator.comparingLong(entry -> distance(host, current, entry)))
                .limit(16)
                .map(entry -> entry.day().map(day -> "Day " + day.value() + " ").orElse("")
                        + entry.time().formatHHmm() + " " + entry.selectorId())
                .toList();
    }

    private long distance(ForgeEchoTimeHost host, long current, TimeScheduleEntry entry) {
        if (entry.day().isPresent()) {
            long dayDelta = entry.day().orElseThrow().value() - host.snapshot().instant().day().value();
            return dayDelta * io.github.kxyr0.echotime.api.value.TimeOfDay.MICROS_PER_DAY
                    + entry.time().microseconds() - current;
        }
        long delta = entry.time().microseconds() - current;
        return delta >= 0L ? delta : delta + io.github.kxyr0.echotime.api.value.TimeOfDay.MICROS_PER_DAY;
    }
}
