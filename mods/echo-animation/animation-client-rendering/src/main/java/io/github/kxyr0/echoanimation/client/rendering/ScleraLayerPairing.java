package io.github.kxyr0.echoanimation.client.rendering;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves authored pupil/sclera layer pairs without mutating the rig.
 */
final class ScleraLayerPairing {
    Optional<ProjectedFaceLayer> resolveParentSurface(
            ProjectedFaceLayer layer,
            Map<BoneId, BoneDefinition> bones,
            Map<BoneId, List<ProjectedFaceLayer>> byBone
    ) {
        Optional<BoneId> parent = Optional.ofNullable(bones.get(layer.command().boneId()))
                .flatMap(BoneDefinition::parentId);
        if (parent.isEmpty()) {
            return Optional.empty();
        }
        List<ProjectedFaceLayer> surfaces = byBone.getOrDefault(parent.orElseThrow(), List.of()).stream()
                .filter(surface -> surface.command().role() != LayerRole.PUPIL)
                .toList();
        FaceLayerId preferred = FaceLayerId.parse(parent.orElseThrow().value());
        return surfaces.stream()
                .filter(surface -> surface.command().layerId().equals(preferred))
                .findFirst()
                .or(() -> surfaces.stream().findFirst());
    }

    Optional<ProjectedFaceLayer> resolve(
            ProjectedFaceLayer layer,
            Map<FaceLayerId, ProjectedFaceLayer> byLayerId
    ) {
        return scleraLayerId(layer.command().layerId().value())
                .map(FaceLayerId::parse)
                .map(byLayerId::get)
                .filter(Objects::nonNull)
                .filter(surface -> surface.command().role() != LayerRole.PUPIL);
    }

    private Optional<String> scleraLayerId(String pupilLayerId) {
        if (pupilLayerId.startsWith("pupil_")) {
            return Optional.of("sclera_" + pupilLayerId.substring("pupil_".length()));
        }
        if (pupilLayerId.endsWith("_pupil")) {
            String prefix = pupilLayerId.substring(0, pupilLayerId.length() - "_pupil".length());
            return Optional.of(prefix + "_sclera");
        }
        return Optional.empty();
    }
}
