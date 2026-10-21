package io.github.kxyr0.echotime.forge.host;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.identity.TimePauseOwner;
import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.mutation.TimeAddRequest;
import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.mutation.TimeSetRequest;
import io.github.kxyr0.echotime.api.pause.TimePauseResult;
import io.github.kxyr0.echotime.api.pause.TimePauseStatus;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleEntry;
import io.github.kxyr0.echotime.api.schedule.TimeScheduleUpdate;
import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.wait.TimeWaitResult;
import io.github.kxyr0.echotime.api.wait.TimeWaitStatus;
import io.github.kxyr0.echotime.core.pause.PauseLeaseChange;
import io.github.kxyr0.echotime.core.state.TimeWaitDecision;
import io.github.kxyr0.echotime.forge.api.EchoTimeMutationService;
import io.github.kxyr0.echotime.forge.api.EchoTimeQueryService;
import io.github.kxyr0.echotime.forge.api.EchoTimeScheduleService;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceBridge;
import io.github.kxyr0.echotime.forge.api.EchoTimeWaitService;
import java.util.List;
import java.util.Objects;

public final class ForgeEchoTimeServices implements EchoTimeQueryService, EchoTimeMutationService,
        EchoTimeScheduleService, EchoTimeWaitService {
    private final ForgeEchoTimeHost host;

    public ForgeEchoTimeServices(ForgeEchoTimeHost host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    public EchoTimeServiceBridge bridge() {
        return new EchoTimeServiceBridge(this, this, this, this);
    }

    @Override public EchoTimeSnapshot current() { return host.snapshot(); }
    @Override public long derivedVanillaDayTime() { return host.derivedVanillaDayTime(); }
    @Override public TimeMutationResult set(TimeSetRequest request) { return host.set(request); }
    @Override public TimeMutationResult add(TimeAddRequest request) { return host.add(request); }

    @Override
    public TimePauseResult pause(TimePauseOwner owner, TimeMutationId mutationId) {
        return pauseResult(owner, host.pause(owner, mutationId));
    }

    @Override
    public TimePauseResult resume(TimePauseOwner owner, TimeMutationId mutationId) {
        return pauseResult(owner, host.resume(owner, mutationId));
    }

    @Override public boolean replace(TimeScheduleUpdate update) { return host.replaceSchedule(update); }
    @Override public List<TimeScheduleEntry> entries() { return host.scheduleEntries(); }

    @Override
    public TimeWaitResult waitUntil(TimeWaitId waitId, TimeOfDay time) {
        TimeWaitDecision decision = host.waitUntil(waitId, time);
        TimeWaitStatus status = TimeWaitStatus.valueOf(decision.status().name());
        return new TimeWaitResult(waitId, status, time, decision.entry().targetAbsoluteMicroseconds());
    }

    @Override public void complete(TimeWaitId waitId) { host.completeWait(waitId); }

    private TimePauseResult pauseResult(TimePauseOwner owner, PauseLeaseChange change) {
        return new TimePauseResult(TimePauseStatus.valueOf(change.status().name()), owner, host.pauseCount());
    }
}
