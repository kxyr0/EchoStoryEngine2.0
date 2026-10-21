package io.github.kxyr0.echoanimation.api.graph.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable channel declaration inside one animation graph.
 */
public record AnimationChannelDefinition(
        AnimationChannelId id,
        AnimationStateId initialStateId,
        int visualOrder,
        ChannelCompositionPolicy compositionPolicy,
        Set<BoneId> boneMask
) {
    public AnimationChannelDefinition(AnimationChannelId id, AnimationStateId initialStateId) {
        this(id, initialStateId, 0, ChannelCompositionPolicy.OVERRIDE, Set.of());
    }

    public AnimationChannelDefinition {
        id = Objects.requireNonNull(id, "id");
        initialStateId = Objects.requireNonNull(initialStateId, "initialStateId");
        compositionPolicy = Objects.requireNonNull(compositionPolicy, "compositionPolicy");
        boneMask = Set.copyOf(Objects.requireNonNull(boneMask, "boneMask"));
    }
}
