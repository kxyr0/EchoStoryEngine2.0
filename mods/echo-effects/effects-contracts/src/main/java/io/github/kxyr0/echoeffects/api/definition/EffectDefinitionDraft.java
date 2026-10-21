package io.github.kxyr0.echoeffects.api.definition;

import io.github.kxyr0.echoeffects.api.channel.EffectTimelineEvent;
import io.github.kxyr0.echoeffects.api.channel.EffectTrack;
import io.github.kxyr0.echoeffects.api.identity.EffectDefinitionRevision;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.status.EffectLifetimePolicy;
import io.github.kxyr0.echoeffects.api.status.EffectRemovalPolicy;
import io.github.kxyr0.echoeffects.api.status.EffectStackingPolicy;
import io.github.kxyr0.echoeffects.api.time.EffectDuration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EffectDefinitionDraft {
    private final EffectId id;
    private EffectDuration duration = EffectDuration.ticks(20L);
    private EffectStackingPolicy stacking = EffectStackingPolicy.RESTART;
    private EffectLifetimePolicy lifetime = EffectLifetimePolicy.INDEPENDENT;
    private EffectRemovalPolicy removal = EffectRemovalPolicy.FADE_OUT;
    private EffectPresentation presentation;
    private final List<EffectEndCondition> endConditions = new ArrayList<>();
    private final List<EffectTrack> tracks = new ArrayList<>();
    private final List<EffectTimelineEvent> events = new ArrayList<>();

    public EffectDefinitionDraft(EffectId id) {
        this.id = Objects.requireNonNull(id, "id");
        this.presentation = EffectPresentation.fallback(id);
    }

    public void duration(EffectDuration value) {
        duration = Objects.requireNonNull(value, "value");
    }

    public void stacking(EffectStackingPolicy value) {
        stacking = Objects.requireNonNull(value, "value");
    }

    public void lifetime(EffectLifetimePolicy value) {
        lifetime = Objects.requireNonNull(value, "value");
    }

    public void presentation(EffectPresentation value) {
        presentation = Objects.requireNonNull(value, "value");
    }

    public void addEndCondition(EffectEndCondition value) {
        endConditions.add(Objects.requireNonNull(value, "value"));
    }

    public void addTrack(EffectTrack track) {
        tracks.add(Objects.requireNonNull(track, "track"));
    }

    public void addTimelineEvent(EffectTimelineEvent event) {
        events.add(Objects.requireNonNull(event, "event"));
    }

    public EffectDefinition toDefinition(EffectDefinitionRevision revision) {
        return new EffectDefinition(id, revision, duration, stacking, lifetime, removal,
                presentation, endConditions, tracks, events);
    }
}
