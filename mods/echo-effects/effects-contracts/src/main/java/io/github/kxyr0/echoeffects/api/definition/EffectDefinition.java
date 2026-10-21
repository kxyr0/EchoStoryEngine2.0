package io.github.kxyr0.echoeffects.api.definition;

import io.github.kxyr0.echoeffects.api.channel.EffectTimelineEvent;
import io.github.kxyr0.echoeffects.api.channel.EffectTrack;
import io.github.kxyr0.echoeffects.api.identity.EffectDefinitionRevision;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.status.EffectLifetimePolicy;
import io.github.kxyr0.echoeffects.api.status.EffectRemovalPolicy;
import io.github.kxyr0.echoeffects.api.status.EffectStackingPolicy;
import io.github.kxyr0.echoeffects.api.time.EffectDuration;
import java.util.List;
import java.util.Objects;

public record EffectDefinition(
        EffectId id,
        EffectDefinitionRevision revision,
        EffectDuration duration,
        EffectStackingPolicy stackingPolicy,
        EffectLifetimePolicy lifetimePolicy,
        EffectRemovalPolicy removalPolicy,
        EffectPresentation presentation,
        List<EffectEndCondition> endConditions,
        List<EffectTrack> tracks,
        List<EffectTimelineEvent> timelineEvents
) {
    public EffectDefinition(
            EffectId id,
            EffectDefinitionRevision revision,
            EffectDuration duration,
            EffectStackingPolicy stackingPolicy,
            EffectLifetimePolicy lifetimePolicy,
            EffectRemovalPolicy removalPolicy,
            List<EffectEndCondition> endConditions,
            List<EffectTrack> tracks,
            List<EffectTimelineEvent> timelineEvents
    ) {
        this(id, revision, duration, stackingPolicy, lifetimePolicy, removalPolicy,
                EffectPresentation.fallback(id), endConditions, tracks, timelineEvents);
    }

    public EffectDefinition {
        id = Objects.requireNonNull(id, "id");
        revision = Objects.requireNonNull(revision, "revision");
        duration = Objects.requireNonNull(duration, "duration");
        stackingPolicy = Objects.requireNonNull(stackingPolicy, "stackingPolicy");
        lifetimePolicy = Objects.requireNonNull(lifetimePolicy, "lifetimePolicy");
        removalPolicy = Objects.requireNonNull(removalPolicy, "removalPolicy");
        presentation = Objects.requireNonNull(presentation, "presentation");
        endConditions = List.copyOf(Objects.requireNonNull(endConditions, "endConditions"));
        tracks = List.copyOf(Objects.requireNonNull(tracks, "tracks"));
        timelineEvents = List.copyOf(Objects.requireNonNull(timelineEvents, "timelineEvents"));
    }
}
