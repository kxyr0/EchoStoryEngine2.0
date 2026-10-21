package io.github.kxyr0.echobackgroundnpc.time;

import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.core.schedule.ScheduleCatalogIndex;
import io.github.kxyr0.echobackgroundnpc.core.simulation.BackgroundAgentRuntime;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeLifecycleEvent;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeLifecyclePhase;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceBatchEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

/** Indexed Echo Time adapter; it contains no per-agent HH:mm tick polling. */
public final class EchoTimeBackgroundAdapter {
    public static final String PROVIDER_ID = "echo_background_npc:schedule";
    private final BackgroundAgentRuntime runtime;
    private final Consumer<BackgroundAgentRuntime.CrossingResult> crossings;

    public EchoTimeBackgroundAdapter(BackgroundAgentRuntime runtime,
            Consumer<BackgroundAgentRuntime.CrossingResult> crossings) {
        this.runtime = runtime;
        this.crossings = crossings;
    }

    public boolean publish(MinecraftServer server) {
        Optional<io.github.kxyr0.echotime.forge.api.EchoTimeServiceBridge> service =
                EchoTimeServiceAccess.find(server);
        if (service.isEmpty()) return false;
        List<TimeScheduleEntry> entries = runtime.schedules().schedules().values().stream()
                .flatMap(value -> value.entries().stream()).map(this::adapt).toList();
        return service.orElseThrow().schedule().replace(new TimeScheduleUpdate(PROVIDER_ID,
                runtime.catalog().revision(), entries));
    }

    public void occurrence(EchoTimeOccurrenceBatchEvent event) {
        event.occurrences().stream().filter(value -> value.entry().generation().equals(generation()))
                .forEach(value -> crossings.accept(runtime.cross(value.entry().selectorId(),
                        value.occurrenceId().value(), presentable(value.entry().selectorId()))));
    }

    public void lifecycle(EchoTimeLifecycleEvent event) {
        if (event.phase() == EchoTimeLifecyclePhase.INITIALIZED || event.phase() == EchoTimeLifecyclePhase.SET
                || event.phase() == EchoTimeLifecyclePhase.RATE_CHANGED) {
            runtime.recomputeAll(minute(event.snapshot().instant().timeOfDay()), "ECHO_TIME_" + event.phase())
                    .forEach(value -> crossings.accept(new BackgroundAgentRuntime.CrossingResult(
                            Optional.of(value), Optional.empty())));
        }
    }

    private TimeScheduleEntry adapt(BackgroundScheduleEntry entry) {
        return new TimeScheduleEntry(entry.selectorId(), new TimeOfDay(entry.minuteOfDay()
                * TimeOfDay.MICROS_PER_MINUTE), Optional.empty(), generation(), entry.oneShot() ? 5 : 0);
    }
    private boolean presentable(String selectorId) {
        return runtime.schedules().occurrence(selectorId).map(ScheduleCatalogIndex.ScheduledAgentEntry::agentId)
                .flatMap(runtime::state).map(value -> value.materialization().name().equals("MATERIALIZED"))
                .orElse(false);
    }
    private String generation() { return PROVIDER_ID + "/" + runtime.catalog().revision(); }
    private int minute(TimeOfDay value) { return value.hour() * 60 + value.minute(); }
}
