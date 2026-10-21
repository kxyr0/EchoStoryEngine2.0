package io.github.kxyr0.echoanimation.api.profile.definition;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationGraphId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileFingerprint;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Authored subject creation profile.
 */
public record AnimationProfileDefinition(
        AnimationProfileId id,
        AnimationRigId rigId,
        AnimationGraphId graphId,
        Map<AnimationChannelId, AnimationStateId> initialStates,
        Set<AnimationChannelId> requiredChannels,
        AnimationRenderModelKind renderModelKind,
        Map<String, String> metadata,
        AnimationProfileFingerprint fingerprint
) {
    public AnimationProfileDefinition {
        id = Objects.requireNonNull(id, "id");
        rigId = Objects.requireNonNull(rigId, "rigId");
        graphId = Objects.requireNonNull(graphId, "graphId");
        initialStates = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(initialStates, "initialStates")));
        requiredChannels = Set.copyOf(Objects.requireNonNull(requiredChannels, "requiredChannels"));
        renderModelKind = Objects.requireNonNull(renderModelKind, "renderModelKind");
        metadata = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(metadata, "metadata")));
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        if (!initialStates.keySet().containsAll(requiredChannels)) {
            throw new IllegalArgumentException("Profile required channels must have initial states.");
        }
    }
}
