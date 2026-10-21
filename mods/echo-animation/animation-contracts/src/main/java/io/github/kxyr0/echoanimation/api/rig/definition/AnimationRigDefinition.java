package io.github.kxyr0.echoanimation.api.rig.definition;

import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import java.util.List;
import java.util.Objects;

/**
 * Immutable rig contract independent from Minecraft renderer internals.
 */
public record AnimationRigDefinition(
        AnimationRigId id,
        long revision,
        AnimationRigKind kind,
        AnimationSkinVariant skinVariant,
        List<AnimationBoneDefinition> bones,
        List<AnimationBoneGeometry> geometry,
        List<AnimationAttachmentSocketDefinition> sockets,
        String semanticFingerprint
) {
    public AnimationRigDefinition {
        id = Objects.requireNonNull(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        skinVariant = Objects.requireNonNull(skinVariant, "skinVariant");
        bones = List.copyOf(Objects.requireNonNull(bones, "bones"));
        geometry = List.copyOf(Objects.requireNonNull(geometry, "geometry"));
        sockets = List.copyOf(Objects.requireNonNull(sockets, "sockets"));
        semanticFingerprint = Objects.requireNonNull(semanticFingerprint, "semanticFingerprint");
        if (revision <= 0L) {
            throw new IllegalArgumentException("revision must be positive.");
        }
    }
}
