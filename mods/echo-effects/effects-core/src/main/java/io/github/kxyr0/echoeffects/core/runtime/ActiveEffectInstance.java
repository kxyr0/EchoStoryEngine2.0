package io.github.kxyr0.echoeffects.core.runtime;

import io.github.kxyr0.echoeffects.api.channel.EffectChannelContribution;
import io.github.kxyr0.echoeffects.api.channel.EffectTimelineEvent;
import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.identity.EffectInstanceId;
import io.github.kxyr0.echoeffects.api.status.EffectOutcome;
import io.github.kxyr0.echoeffects.api.status.EffectStatus;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.api.view.ActiveEffectView;
import io.github.kxyr0.echoeffects.core.curve.EffectCurveSampler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ActiveEffectInstance {
    private final EffectInstanceId instanceId;
    private final EffectDefinition definition;
    private final EffectTargetRef target;
    private final Set<Integer> dispatchedTimelineEvents = new HashSet<>();
    private long startTick;
    private double intensity;
    private EffectStatus status = EffectStatus.APPLYING;
    private Optional<EffectOutcome> outcome = Optional.empty();

    ActiveEffectInstance(EffectInstanceId instanceId, EffectDefinition definition,
                         EffectTargetRef target, long startTick, double intensity) {
        this.instanceId = instanceId;
        this.definition = definition;
        this.target = target;
        this.startTick = startTick;
        this.intensity = intensity;
    }

    public EffectDefinition definition() {
        return definition;
    }

    EffectInstanceId instanceId() {
        return instanceId;
    }

    EffectTargetRef target() {
        return target;
    }

    public boolean terminal() {
        return status == EffectStatus.COMPLETED || status == EffectStatus.REMOVED
                || status == EffectStatus.CANCELLED || status == EffectStatus.FAILED;
    }

    public void tick(long serverTick) {
        if (terminal()) {
            return;
        }
        status = EffectStatus.ACTIVE;
        if (!definition.duration().infiniteDuration() && elapsed(serverTick) >= definition.duration().ticks()) {
            status = EffectStatus.COMPLETED;
            outcome = Optional.of(EffectOutcome.COMPLETED);
        }
    }

    public void refresh(long serverTick) {
        startTick = serverTick;
        status = EffectStatus.ACTIVE;
        outcome = Optional.empty();
        dispatchedTimelineEvents.clear();
    }

    public void stackIntensity(double delta) {
        intensity = Math.min(4.0D, intensity + delta);
    }

    public void remove(EffectOutcome terminalOutcome) {
        status = terminalOutcome == EffectOutcome.CANCELLED ? EffectStatus.CANCELLED : EffectStatus.REMOVED;
        outcome = Optional.of(terminalOutcome);
    }

    public List<EffectChannelContribution> contributions(long serverTick, EffectCurveSampler sampler) {
        double effectTick = elapsed(serverTick);
        return definition.tracks().stream()
                .map(track -> new EffectChannelContribution(track.channelId(),
                        sampler.sample(track.curve(), effectTick) * intensity))
                .toList();
    }

    public ActiveEffectView view(long serverTick) {
        return new ActiveEffectView(instanceId, definition.id(), target, status, outcome,
                elapsed(serverTick), definition.duration(), intensity);
    }

    List<EffectTimelineEvent> drainTimelineEvents(long serverTick) {
        if (terminal()) {
            return List.of();
        }
        long effectTick = elapsed(serverTick);
        List<EffectTimelineEvent> result = new ArrayList<>();
        List<EffectTimelineEvent> timelineEvents = definition.timelineEvents();
        for (int index = 0; index < timelineEvents.size(); index++) {
            EffectTimelineEvent event = timelineEvents.get(index);
            if (effectTick >= event.time().ticks() && dispatchedTimelineEvents.add(index)) {
                result.add(event);
            }
        }
        return result;
    }

    private long elapsed(long serverTick) {
        return Math.max(0L, serverTick - startTick);
    }
}
