package io.github.kxyr0.echotime.forge.host;

import io.github.kxyr0.echotime.api.config.TimeConfigResult;
import io.github.kxyr0.echotime.api.config.TimeSpeedConfig;
import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeMutationStatus;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.core.mutation.MutationExecution;
import io.github.kxyr0.echotime.core.pause.PauseLeaseChange;
import io.github.kxyr0.echotime.core.runtime.EchoTimeRuntime;
import io.github.kxyr0.echotime.core.runtime.TimeTickResult;
import io.github.kxyr0.echotime.core.state.TimeWaitDecision;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeLifecyclePhase;
import io.github.kxyr0.echotime.forge.config.TimeConfigPublication;
import io.github.kxyr0.echotime.forge.config.TimeConfigWatcher;
import io.github.kxyr0.echotime.forge.config.TimeSpeedConfigLoader;
import io.github.kxyr0.echotime.forge.daylight.DaylightOwnership;
import io.github.kxyr0.echotime.forge.network.EchoTimeNetwork;
import io.github.kxyr0.echotime.forge.persistence.EchoTimeSavedData;
import java.io.IOException;
import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class ForgeEchoTimeHost implements AutoCloseable {
    private static final int PERIODIC_SYNC_TICKS = 20, RELOAD_DEBOUNCE_TICKS = 8;
    private final MinecraftServer server;
    private final EchoTimeNetwork network;
    private final TimeSpeedConfigLoader loader;
    private final TimeConfigWatcher watcher;
    private final TimeConfigPublication configs;
    private final EchoTimeSavedData savedData;
    private final EchoTimeRuntime runtime;
    private final DaylightOwnership daylight = new DaylightOwnership();
    private final EchoTimeTrace trace = new EchoTimeTrace();
    private final EchoTimeEventPublisher events;
    private long reloadAt = Long.MAX_VALUE, lastConflictDiagnostic = Long.MIN_VALUE;

    public ForgeEchoTimeHost(MinecraftServer server, EchoTimeNetwork network) throws IOException {
        this.server = server;
        this.network = network;
        events = new EchoTimeEventPublisher(server, trace);
        loader = TimeSpeedConfigLoader.gameDirectory();
        loader.seedIfMissing();
        TimeConfigResult first = loader.load(1L);
        TimeSpeedConfig provisional = first.config().orElseGet(EchoTimeBootstrap::defaultConfig);
        savedData = EchoTimeSavedData.get(server.overworld(), () -> EchoTimeBootstrap.initialState(provisional));
        TimeSpeedConfig fallback = EchoTimeBootstrap.configFromState(savedData.state());
        configs = new TimeConfigPublication(fallback);
        long nextRevision = savedData.newlyCreated() ? 1L : savedData.state().configRevision() + 1L;
        TimeConfigResult candidate = loader.load(nextRevision);
        configs.commit(candidate);
        runtime = new EchoTimeRuntime(savedData.state(), configs.active().maximumCrossedEvents());
        if (candidate.successful() && !savedData.newlyCreated()) {
            runtime.applyConfig(configs.active().rate(), configs.active().running(), configs.active().revision());
            trace.add("ECHO_TIME_INITIAL_VALUE_IGNORED_EXISTING_CLOCK", runtime.instant().toString());
        }
        daylight.acquire(server, configs.active());
        daylight.sync(server, configs.active(), runtime.instant());
        watcher = new TimeConfigWatcher(loader.path());
        watcher.start();
        persist();
        trace.addInstant("CLOCK_INITIALIZED", runtime.instant());
        lifecycle(EchoTimeLifecyclePhase.INITIALIZED);
    }

    public void tick() {
        long tick = server.getTickCount();
        if (watcher.consumeChange()) reloadAt = tick + RELOAD_DEBOUNCE_TICKS;
        if (tick >= reloadAt) {
            reloadAt = Long.MAX_VALUE;
            reload();
        }
        TimeTickResult result = runtime.tick(tick);
        if (result.transition().advanced()) {
            persist();
            events.occurrences(result.occurrences());
            result.completedWaits().forEach(events::waitCompleted);
            lifecycle(EchoTimeLifecyclePhase.ADVANCED);
            if (!result.transition().previous().day().equals(result.transition().current().day())) {
                trace.addInstant("DAY_ROLLOVER", result.transition().current());
                lifecycle(EchoTimeLifecyclePhase.DAY_CHANGED);
            }
        }
        var sync = daylight.sync(server, configs.active(), runtime.instant());
        if (sync.conflict() && tick - lastConflictDiagnostic >= 100L) {
            lastConflictDiagnostic = tick;
            trace.add(sync.gameruleConflict() ? "ECHO_TIME_DAYLIGHT_OWNERSHIP_CONFLICT"
                    : "ECHO_TIME_EXTERNAL_DAYTIME_MUTATION", sync.externallyMutatedDimensions().toString());
        }
        if (tick % PERIODIC_SYNC_TICKS == 0L) network.broadcast(snapshot());
    }

    TimeMutationResult set(TimeSetRequest request) {
        return mutation(runtime.set(request, server.getTickCount()), EchoTimeLifecyclePhase.SET);
    }

    TimeMutationResult add(TimeAddRequest request) {
        return mutation(runtime.add(request, server.getTickCount()), EchoTimeLifecyclePhase.ADDED);
    }

    PauseLeaseChange pause(TimePauseOwner owner, TimeMutationId mutationId) {
        PauseLeaseChange result = runtime.pause(owner, mutationId);
        persistNow();
        trace.add("PAUSED", owner.value());
        lifecycle(EchoTimeLifecyclePhase.PAUSED);
        network.broadcast(snapshot());
        return result;
    }

    PauseLeaseChange resume(TimePauseOwner owner, TimeMutationId mutationId) {
        PauseLeaseChange result = runtime.resume(owner);
        persistNow();
        trace.add("RESUMED", owner.value() + " via " + mutationId.value());
        lifecycle(EchoTimeLifecyclePhase.RESUMED);
        network.broadcast(snapshot());
        return result;
    }

    TimeWaitDecision waitUntil(TimeWaitId id, TimeOfDay time) {
        TimeWaitDecision decision = runtime.waitUntil(id, time);
        persistNow();
        return decision;
    }

    void completeWait(TimeWaitId id) { runtime.completeWait(id); persistNow(); }
    boolean replaceSchedule(TimeScheduleUpdate update) { return runtime.replaceSchedule(update); }
    public List<TimeScheduleEntry> scheduleEntries() { return runtime.scheduleEntries(); }
    public EchoTimeSnapshot snapshot() { return runtime.snapshot(server.getTickCount()); }
    public long derivedVanillaDayTime() { return daylight.derived(runtime.instant()); }
    int pauseCount() { return runtime.state().pauseLeases().size(); }
    public EchoTimeTrace trace() { return trace; }
    public TimeSpeedConfig config() { return configs.active(); }
    public boolean owns(MinecraftServer candidate) { return server == candidate; }
    public List<io.github.kxyr0.echotime.api.identity.TimePauseLease> pauseLeases() {
        return runtime.state().pauseLeases();
    }
    public List<io.github.kxyr0.echotime.api.config.TimeConfigDiagnostic> diagnostics() {
        return configs.diagnostics();
    }

    public void reload() {
        TimeConfigResult candidate = loader.load(configs.active().revision() + 1L);
        if (!configs.commit(candidate)) {
            trace.add("ECHO_TIME_LAST_KNOWN_GOOD_RETAINED", configs.diagnostics().toString());
            return;
        }
        runtime.applyConfig(configs.active().rate(), configs.active().running(), configs.active().revision());
        daylight.release(server);
        daylight.acquire(server, configs.active());
        persist();
        trace.add("RATE_CHANGED", configs.active().rate().toString());
        lifecycle(EchoTimeLifecyclePhase.RATE_CHANGED);
        network.broadcast(snapshot());
    }

    public void resync() {
        network.broadcast(snapshot());
        trace.add("CLIENT_RESYNC", "manual");
    }

    @Override public void close() throws IOException { daylight.release(server); watcher.close(); persist(); }

    private TimeMutationResult mutation(MutationExecution execution, EchoTimeLifecyclePhase phase) {
        if (execution.result().status() == TimeMutationStatus.APPLIED) {
            persist();
            events.occurrences(execution.occurrences());
            runtime.completedWaits().forEach(events::waitCompleted);
            persistNow();
            trace.add(phase.name(), execution.result().mutationId().value());
            lifecycle(phase);
            daylight.sync(server, configs.active(), runtime.instant());
            network.broadcast(snapshot());
        } else if (execution.result().status() != TimeMutationStatus.DUPLICATE
                && execution.result().status() != TimeMutationStatus.IDEMPOTENCY_CONFLICT) {
            persistNow();
        }
        return execution.result();
    }

    private void lifecycle(EchoTimeLifecyclePhase phase) { events.lifecycle(phase, snapshot()); }

    private void persist() { savedData.update(runtime.state()); }

    private void persistNow() { persist(); server.overworld().getDataStorage().save(); }

}
