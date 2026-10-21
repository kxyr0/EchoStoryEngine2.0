package io.github.kxyr0.echoanimation.definition.validation;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validates cross-field face rig invariants.
 */
public final class FaceRigValidator {
    public void validate(FaceRigDefinition rig) {
        Map<BoneId, BoneDefinition> bones = rig.hierarchy().bones().stream()
                .collect(Collectors.toMap(BoneDefinition::id, bone -> bone));
        Map<FaceLayerId, FaceLayerDefinition> layers = new HashMap<>();
        for (FaceLayerDefinition layer : rig.layers()) {
            if (!bones.containsKey(layer.boneId())) {
                throw new IllegalArgumentException("Face layer ссылается на unknown bone: " + layer.boneId());
            }
            if (layers.putIfAbsent(layer.id(), layer) != null) {
                throw new IllegalArgumentException("FaceLayerId объявлен повторно: " + layer.id());
            }
            layer.clipRect().ifPresent(clip -> {
                if (!clip.fitsInside(64, 64)) {
                    throw new IllegalArgumentException("Clip rect выходит за 64x64 atlas: " + layer.id());
                }
            });
        }
        validateCanonicalParents(bones);
        validateBlinkControls(rig.controls(), bones);
        validatePupilLayerPairs(layers);
        validateDrawOrderConflicts(rig.layers());
    }

    private void validateBlinkControls(List<FaceControlDefinition> controls, Map<BoneId, BoneDefinition> bones) {
        for (FaceControlDefinition control : controls) {
            if (!control.idleBlink()) {
                continue;
            }
            requireBone(bones, control.blinkUpperBone(), "blink upper bone");
            requireBone(bones, control.blinkLowerBone(), "blink lower bone");
        }
    }

    private void requireBone(Map<BoneId, BoneDefinition> bones, BoneId bone, String label) {
        if (!bones.containsKey(bone)) {
            throw new IllegalArgumentException(label + " missing: " + bone);
        }
    }

    private void validateCanonicalParents(Map<BoneId, BoneDefinition> bones) {
        requireParentIfPresent(bones, "left_eye_root", "face_root");
        requireParentIfPresent(bones, "right_eye_root", "face_root");
        requireParentIfPresent(bones, "left_sclera", "left_eye_root");
        requireParentIfPresent(bones, "right_sclera", "right_eye_root");
        requireParentIfPresent(bones, "left_pupil", "left_sclera");
        requireParentIfPresent(bones, "right_pupil", "right_sclera");
        requireParentIfPresent(bones, "left_brow", "face_root");
        requireParentIfPresent(bones, "right_brow", "face_root");
        requireParentIfPresent(bones, "glasses_root", "face_root");
    }

    private void requireParentIfPresent(Map<BoneId, BoneDefinition> bones, String child, String parent) {
        BoneId childId = BoneId.parse(child);
        BoneId parentId = BoneId.parse(parent);
        if (!bones.containsKey(childId)) {
            return;
        }
        if (!bones.containsKey(parentId)) {
            throw new IllegalArgumentException("Face bone parent missing: " + childId + " requires " + parentId + ".");
        }
        Optional<BoneId> actual = bones.get(childId).parentId();
        if (!actual.equals(Optional.of(parentId))) {
            throw new IllegalArgumentException("Face bone parent mismatch: " + childId + " должен быть child "
                    + parentId + ".");
        }
    }

    private void validatePupilLayerPairs(Map<FaceLayerId, FaceLayerDefinition> layers) {
        requireScleraLayerIfPupilPresent(layers, "pupil_left", "sclera_left");
        requireScleraLayerIfPupilPresent(layers, "pupil_right", "sclera_right");
        requireScleraLayerIfPupilPresent(layers, "left_pupil", "left_sclera");
        requireScleraLayerIfPupilPresent(layers, "right_pupil", "right_sclera");
    }

    private void requireScleraLayerIfPupilPresent(
            Map<FaceLayerId, FaceLayerDefinition> layers,
            String pupil,
            String sclera
    ) {
        FaceLayerDefinition pupilLayer = layers.get(FaceLayerId.parse(pupil));
        if (pupilLayer == null || pupilLayer.role() != LayerRole.PUPIL) {
            return;
        }
        FaceLayerDefinition scleraLayer = layers.get(FaceLayerId.parse(sclera));
        if (scleraLayer == null || scleraLayer.role() == LayerRole.PUPIL) {
            throw new IllegalArgumentException("Pupil layer " + pupil + " requires matching sclera layer " + sclera
                    + ".");
        }
    }

    private void validateDrawOrderConflicts(List<FaceLayerDefinition> layers) {
        Map<Integer, List<FaceLayerDefinition>> byOrder = new HashMap<>();
        for (FaceLayerDefinition layer : layers) {
            byOrder.computeIfAbsent(layer.drawOrder(), ignored -> new java.util.ArrayList<>()).add(layer);
        }
        for (List<FaceLayerDefinition> sameOrder : byOrder.values()) {
            validateSameOrderGroup(sameOrder);
        }
    }

    private void validateSameOrderGroup(List<FaceLayerDefinition> layers) {
        for (int left = 0; left < layers.size(); left++) {
            for (int right = left + 1; right < layers.size(); right++) {
                if (overlaps(layers.get(left), layers.get(right))) {
                    throw new IllegalArgumentException("Face layers пересекаются с одинаковым drawOrder: "
                            + layers.get(left).id() + " и " + layers.get(right).id());
                }
            }
        }
    }

    private boolean overlaps(FaceLayerDefinition left, FaceLayerDefinition right) {
        Bounds a = bounds(left);
        Bounds b = bounds(right);
        return a.left() < b.right() && a.right() > b.left() && a.bottom() < b.top() && a.top() > b.bottom();
    }

    private Bounds bounds(FaceLayerDefinition layer) {
        double width = layer.atlasRect().width() / 16.0D * Math.abs(layer.placement().scale().x());
        double height = layer.atlasRect().height() / 16.0D * Math.abs(layer.placement().scale().y());
        double centerX = layer.placement().translation().x();
        double centerY = layer.placement().translation().y();
        return new Bounds(centerX - width / 2.0D, centerX + width / 2.0D,
                centerY - height / 2.0D, centerY + height / 2.0D);
    }

    private record Bounds(double left, double right, double bottom, double top) {
    }
}
