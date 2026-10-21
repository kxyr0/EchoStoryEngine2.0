package io.github.kxyr0.echotime.core.runtime;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.schedule.TimeCrossingReason;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.core.clock.ClockTransition;
import io.github.kxyr0.echotime.core.clock.EchoClock;
import io.github.kxyr0.echotime.core.mutation.MutationExecution;
import io.github.kxyr0.echotime.core.mutation.TimeMutationExecutor;
import io.github.kxyr0.echotime.core.mutation.TimeMutationLedger;
import io.github.kxyr0.echotime.core.pause.PauseLeaseChange;
import io.github.kxyr0.echotime.core.pause.TimePauseRegistry;
import io.github.kxyr0.echotime.core.schedule.ScheduleCrossingResult;
import io.github.kxyr0.echotime.core.schedule.TimeOccurrenceSchedule;
import io.github.kxyr0.echotime.core.state.EchoTimeState;
import io.github.kxyr0.echotime.core.state.TimeWait;
import io.github.kxyr0.echotime.core.state.TimeWaitDecision;
import io.github.kxyr0.echotime.core.state.TimeWaitRegistry;
import java.util.List;
import java.util.Objects;

public final class EchoTimeRuntime {
    public static final int STATE_SCHEMA = 1;
    private final EchoClock clock;
    private final TimePauseRegistry pauses = new TimePauseRegistry();
    private final TimeOccurrenceSchedule schedule = new TimeOccurrenceSchedule();
    private final TimeMutationLedger mutations = new TimeMutationLedger();
    private final TimeWaitRegistry waits = new TimeWaitRegistry();
    private final TimeMutationExecutor mutationExecutor;
    private final int maximumCrossedEvents;

    public EchoTimeRuntime(EchoTimeState state, int maximumCrossedEvents) {
        Objects.requireNonNull(state, "state");
        if (maximumCrossedEvents <= 0) throw new IllegalArgumentException("maximumCrossedEvents must be positive.");
        this.maximumCrossedEvents = maximumCrossedEvents;
        clock = new EchoClock(state.instant(), state.rate(), state.fixedPointRemainder(),
                state.configuredRunning(), state.configRevision());
        pauses.restore(state.pauseLeases());
        mutations.restore(state.mutationReceipts());
        waits.restore(state.waits());
        mutationExecutor = new TimeMutationExecutor(clock, schedule, mutations, maximumCrossedEvents);
    }

    public TimeTickResult tick(long serverTick) {
        ClockTransition transition = clock.advance(serverTick, pauses.paused());
        ScheduleCrossingResult crossing = transition.advanced()
                ? schedule.crossed(transition.previous(), transition.current(),
                        TimeCrossingReason.NATURAL_ADVANCE, maximumCrossedEvents)
                : new ScheduleCrossingResult(List.of(), false);
        List<TimeWait> completed = waits.completed(clock.instant());
        return new TimeTickResult(transition, crossing.occurrences(), completed, crossing.limitExceeded());
    }

    public MutationExecution set(TimeSetRequest request, long serverTick) {
        return mutationExecutor.set(request, serverTick);
    }

    public MutationExecution add(TimeAddRequest request, long serverTick) {
        return mutationExecutor.add(request, serverTick);
    }

    public PauseLeaseChange pause(TimePauseOwner owner, TimeMutationId mutationId) {
        return pauses.acquire(owner, mutationId);
    }

    public PauseLeaseChange resume(TimePauseOwner owner) {
        return pauses.release(owner);
    }

    public TimeWaitDecision waitUntil(TimeWaitId id, TimeOfDay time) {
        return waits.register(id, time, clock.instant());
    }

    public void completeWait(TimeWaitId id) {
        waits.remove(id);
    }

    public List<TimeWait> completedWaits() { return waits.completed(clock.instant()); }

    public boolean replaceSchedule(TimeScheduleUpdate update) {
        return schedule.replace(update);
    }

    public void applyConfig(TimeRate rate, boolean running, long revision) {
        clock.applyConfig(rate, running, revision);
    }

    public EchoTimeSnapshot snapshot(long serverTick) {
        return clock.snapshot(serverTick, pauses.paused());
    }

    public EchoInstant instant() {
        return clock.instant();
    }

    public List<TimeScheduleUpdate> unsupportedScheduleImages() {
        return List.of();
    }

    public List<io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry> scheduleEntries() {
        return schedule.entries();
    }

    public EchoTimeState state() {
        return new EchoTimeState(STATE_SCHEMA, clock.instant(), clock.rate(), clock.remainder(),
                clock.configuredRunning(), clock.configRevision(), pauses.leases(), mutations.receipts(), waits.waits());
    }
}
