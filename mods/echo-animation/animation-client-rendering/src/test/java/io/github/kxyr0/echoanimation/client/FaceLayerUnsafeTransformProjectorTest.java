package io.github.kxyr0.echoanimation.client;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.client.rendering.FaceLayerPoseProjector;
import io.github.kxyr0.echoanimation.client.rendering.ProjectedFaceLayer;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class FaceLayerUnsafeTransformProjectorTest {
    private static final BoneId FACE_ROOT = BoneId.parse("face_root");
    private static final BoneId LEFT_SCLERA = BoneId.parse("left_sclera");
    private static final BoneId LEFT_PUPIL = BoneId.parse("left_pupil");

    @Test
    void rotatedPupilIsNotProjectedBecauseItCannotBeSafelyClipped() {
        var pose = new BonePose(Map.of(LEFT_PUPIL, rotated(15.0D)));
        var projected = new FaceLayerPoseProjector().project(rig(Transform3.IDENTITY), pose, Set.of());

        Assertions.assertFalse(hasLayer(projected, "left_pupil"));
    }

    @Test
    void pupilWithRotatedScleraSurfaceIsNotProjected() {
        var projected = new FaceLayerPoseProjector().project(rig(rotated(15.0D)), new BonePose(Map.of()), Set.of());

        Assertions.assertFalse(hasLayer(projected, "left_pupil"));
    }

    private FaceRigDefinition rig(Transform3 scleraPlacement) {
        return new FaceRigDefinition(FaceRigId.parse("jdh:john_father"),
                new BoneHierarchy(List.of(
                        BoneDefinition.root(FACE_ROOT),
                        bone(LEFT_SCLERA, FACE_ROOT),
                        bone(LEFT_PUPIL, LEFT_SCLERA))),
                List.of(
                        layer("left_sclera", LEFT_SCLERA, LayerRole.DECORATION, scleraPlacement, 4),
                        layer("left_pupil", LEFT_PUPIL, LayerRole.PUPIL, Transform3.IDENTITY, 1)),
                List.of(), "fingerprint");
    }

    private Transform3 rotated(double degrees) {
        return new Transform3(Vec3.ZERO, degrees, Vec3.ONE, Vec3.ZERO);
    }

    private BoneDefinition bone(BoneId id, BoneId parent) {
        return new BoneDefinition(id, Optional.of(parent), Transform3.IDENTITY);
    }

    private boolean hasLayer(List<ProjectedFaceLayer> projected, String id) {
        return projected.stream().anyMatch(layer -> layer.command().layerId().equals(FaceLayerId.parse(id)));
    }

    private FaceLayerDefinition layer(String id, BoneId bone, LayerRole role, Transform3 placement, int width) {
        return new FaceLayerDefinition(FaceLayerId.parse(id), bone, FaceTextureId.parse("jdh:face/" + id),
                role, role == LayerRole.PUPIL ? 30 : 20, placement, new IntRect(0, 0, width, 1),
                Optional.empty(), true);
    }
}
