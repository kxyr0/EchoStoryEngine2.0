package io.github.kxyr0.echonpcanimationadapter;

import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter-owned authored binding from NPC identity to animation profile.
 */
public record NpcAnimationBindingDefinition(
        NpcAnimationBindingId id,
        Optional<ActorId> actorId,
        Optional<NpcDefinitionId> definitionId,
        AnimationProfileId profileId,
        String fingerprint
) {
    public NpcAnimationBindingDefinition {
        id = Objects.requireNonNull(id, "id");
        actorId = Objects.requireNonNull(actorId, "actorId");
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        profileId = Objects.requireNonNull(profileId, "profileId");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint").trim();
        if (actorId.isPresent() == definitionId.isPresent()) {
            throw new IllegalArgumentException("Binding must target exactly one actor or NPC definition.");
        }
        if (fingerprint.isEmpty() || fingerprint.length() > 128) {
            throw new IllegalArgumentException("Binding fingerprint must be non-empty and at most 128 chars.");
        }
    }

    public Specificity specificity() {
        return actorId.isPresent() ? Specificity.ACTOR : Specificity.DEFINITION;
    }

    public enum Specificity {
        ACTOR,
        DEFINITION
    }
}
