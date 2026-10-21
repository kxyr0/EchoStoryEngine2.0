package io.github.kxyr0.echoanimation.client.humanoid;

import io.github.kxyr0.echoanimation.api.rig.definition.AnimationAttachmentSocketDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable segmented geometry profile for one vanilla skin variant.
 */
public record SegmentedHumanoidGeometryProfile(
        AnimationSkinVariant skinVariant,
        List<HumanoidSegmentGeometry> geometry,
        List<AnimationAttachmentSocketDefinition> sockets
) {
    public SegmentedHumanoidGeometryProfile {
        skinVariant = Objects.requireNonNull(skinVariant, "skinVariant");
        geometry = List.copyOf(Objects.requireNonNull(geometry, "geometry"));
        sockets = List.copyOf(Objects.requireNonNull(sockets, "sockets"));
    }

    public Optional<HumanoidSegmentGeometry> geometry(HumanoidSegmentId segmentId, boolean overlay) {
        return geometry.stream()
                .filter(entry -> entry.segmentId() == segmentId)
                .filter(entry -> entry.overlay() == overlay)
                .findFirst();
    }
}
