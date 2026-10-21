package io.github.kxyr0.echobackgroundnpc.api.conversation;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundQuestId;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import java.util.Objects;

/** Optional Echo Quest started for an observing player after natural conversation completion. */
public record AmbientQuestOffer(BackgroundQuestId questId, double maximumDistance, BackgroundSourceRef source) {
    public static final double DEFAULT_MAXIMUM_DISTANCE = 12.0D;
    public static final double MAXIMUM_ALLOWED_DISTANCE = 14.0D;

    public AmbientQuestOffer {
        questId = Objects.requireNonNull(questId, "questId");
        source = Objects.requireNonNull(source, "source");
        if (!Double.isFinite(maximumDistance) || maximumDistance <= 0.0D
                || maximumDistance > MAXIMUM_ALLOWED_DISTANCE) {
            throw new IllegalArgumentException("StartQuest range must be in (0, 14].");
        }
    }
}
