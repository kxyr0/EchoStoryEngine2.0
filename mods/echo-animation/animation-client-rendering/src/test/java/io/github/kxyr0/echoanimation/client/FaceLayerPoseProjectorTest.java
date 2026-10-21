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

final class FaceLayerPoseProjectorTest {
    private static final BoneId FACE_ROOT = BoneId.parse("face_root");
    private static final BoneId LEFT_EYE_ROOT = BoneId.parse("left_eye_root");
    private static final BoneId LEFT_SCLERA = BoneId.parse("left_sclera");
    private static final BoneId LEFT_PUPIL = BoneId.parse("left_pupil");
    private static final BoneId RIGHT_EYE_ROOT = BoneId.parse("right_eye_root");
    private static final BoneId RIGHT_SCLERA = BoneId.parse("right_sclera");
    private static final BoneId RIGHT_PUPIL = BoneId.parse("right_pupil");

    @Test
    void pupilLayerInheritsScleraParentTransform() {
        FaceRigDefinition rig = rig();
        BonePose pose = new BonePose(Map.of(
                LEFT_SCLERA, Transform3.IDENTITY.withTranslation(new Vec3(0.2D, 0.0D, 0.0D)),
                LEFT_PUPIL, Transform3.IDENTITY.withTranslation(new Vec3(0.05D, 0.0D, 0.0D))));

        var projected = new FaceLayerPoseProjector().project(rig, pose, Set.of());
        var pupil = layer(projected, "left_pupil");

        Assertions.assertEquals(0.26D, pupil.transform().translation().x(), 0.0001D);
        Assertions.assertEquals(0.0D, pupil.transform().translation().y(), 0.0001D);
    }

    @Test
    void rightPupilLayerInheritsScleraParentTransform() {
        FaceRigDefinition rig = rig();
        BonePose pose = new BonePose(Map.of(
                RIGHT_SCLERA, Transform3.IDENTITY.withTranslation(new Vec3(-0.2D, 0.0D, 0.0D)),
                RIGHT_PUPIL, Transform3.IDENTITY.withTranslation(new Vec3(-0.05D, 0.0D, 0.0D))));

        var projected = new FaceLayerPoseProjector().project(rig, pose, Set.of());
        var pupil = layer(projected, "right_pupil");

        Assertions.assertEquals(-0.26D, pupil.transform().translation().x(), 0.0001D);
        Assertions.assertEquals(0.0D, pupil.transform().translation().y(), 0.0001D);
    }

    @Test
    void hiddenLayerIsNotProjected() {
        var projected = new FaceLayerPoseProjector().project(rig(), new BonePose(Map.of()),
                Set.of(FaceLayerId.parse("left_pupil")));
        Assertions.assertTrue(projected.stream()
                .noneMatch(layer -> layer.command().layerId().equals(FaceLayerId.parse("left_pupil"))));
    }

    @Test
    void pupilLayerIsClippedByParentScleraSurface() {
        FaceRigDefinition rig = rig();
        BonePose pose = new BonePose(Map.of(LEFT_PUPIL,
                Transform3.IDENTITY.withTranslation(new Vec3(0.11D, 0.0D, 0.0D))));

        var pupil = layer(new FaceLayerPoseProjector().project(rig, pose, Set.of()), "left_pupil");

        Assertions.assertTrue(pupil.transform().translation().x() < 0.12D);
        Assertions.assertTrue(pupil.transform().translation().x() <= 0.125D);
    }

    @Test
    void pupilLayerPrefersNamedParentScleraSurface() {
        FaceRigDefinition rig = rigWithDistractingParentLayer();

        var projected = new FaceLayerPoseProjector().project(rig, new BonePose(Map.of()), Set.of());

        Assertions.assertTrue(hasLayer(projected, "left_pupil"));
    }

    @Test
    void rightPupilLayerIsClippedByParentScleraSurface() {
        FaceRigDefinition rig = rig();
        BonePose pose = new BonePose(Map.of(RIGHT_PUPIL,
                Transform3.IDENTITY.withTranslation(new Vec3(-0.11D, 0.0D, 0.0D))));

        var pupil = layer(new FaceLayerPoseProjector().project(rig, pose, Set.of()), "right_pupil");

        Assertions.assertTrue(pupil.transform().translation().x() > -0.12D);
        Assertions.assertTrue(pupil.transform().translation().x() >= -0.125D);
    }

    @Test
    void pupilLayerFullyOutsideParentScleraSurfaceIsClamped() {
        FaceRigDefinition rig = rig();
        BonePose pose = new BonePose(Map.of(LEFT_PUPIL,
                Transform3.IDENTITY.withTranslation(new Vec3(0.25D, 0.0D, 0.0D))));

        var pupil = layer(new FaceLayerPoseProjector().project(rig, pose, Set.of()), "left_pupil");

        Assertions.assertEquals(0.09375D, pupil.transform().translation().x(), 0.0001D);
    }

    @Test
    void rightPupilLayerFullyOutsideParentScleraSurfaceIsClamped() {
        FaceRigDefinition rig = rig();
        BonePose pose = new BonePose(Map.of(RIGHT_PUPIL,
                Transform3.IDENTITY.withTranslation(new Vec3(-0.25D, 0.0D, 0.0D))));

        var pupil = layer(new FaceLayerPoseProjector().project(rig, pose, Set.of()), "right_pupil");

        Assertions.assertEquals(-0.09375D, pupil.transform().translation().x(), 0.0001D);
    }

    @Test
    void pupilLayerIsClippedByPairedScleraLayerWhenBothUseFaceRoot() {
        var projected = new FaceLayerPoseProjector().project(faceRootLayerRig(), new BonePose(Map.of()), Set.of());
        var left = layer(projected, "pupil_left");
        var right = layer(projected, "pupil_right");

        Assertions.assertTrue(left.transform().translation().x() <= 0.125D);
        Assertions.assertTrue(right.transform().translation().x() >= -0.125D);
    }

    private FaceRigDefinition rig() {
        return new FaceRigDefinition(FaceRigId.parse("jdh:john_father"),
                new BoneHierarchy(List.of(
                        BoneDefinition.root(FACE_ROOT),
                        bone(LEFT_EYE_ROOT, FACE_ROOT),
                        bone(LEFT_SCLERA, LEFT_EYE_ROOT),
                        bone(LEFT_PUPIL, LEFT_SCLERA),
                        bone(RIGHT_EYE_ROOT, FACE_ROOT),
                        bone(RIGHT_SCLERA, RIGHT_EYE_ROOT),
                        bone(RIGHT_PUPIL, RIGHT_SCLERA))),
                List.of(
                        layer("left_sclera", LEFT_SCLERA, LayerRole.DECORATION,
                                Transform3.IDENTITY.withTranslation(new Vec3(0.0D, 0.0D, 0.6D)), 4),
                        layer("left_pupil", LEFT_PUPIL, LayerRole.PUPIL,
                                Transform3.IDENTITY.withTranslation(new Vec3(0.01D, 0.0D, 0.62D)), 1),
                        layer("right_sclera", RIGHT_SCLERA, LayerRole.DECORATION,
                                Transform3.IDENTITY.withTranslation(new Vec3(0.0D, 0.0D, 0.6D)), 4),
                        layer("right_pupil", RIGHT_PUPIL, LayerRole.PUPIL,
                                Transform3.IDENTITY.withTranslation(new Vec3(-0.01D, 0.0D, 0.62D)), 1)),
                List.of(), "fingerprint");
    }

    private FaceRigDefinition rigWithDistractingParentLayer() {
        return new FaceRigDefinition(FaceRigId.parse("jdh:john_father"),
                new BoneHierarchy(List.of(
                        BoneDefinition.root(FACE_ROOT),
                        bone(LEFT_EYE_ROOT, FACE_ROOT),
                        bone(LEFT_SCLERA, LEFT_EYE_ROOT),
                        bone(LEFT_PUPIL, LEFT_SCLERA))),
                List.of(
                        layer("a_shadow", LEFT_SCLERA, LayerRole.DECORATION,
                                Transform3.IDENTITY.withTranslation(new Vec3(1.0D, 0.0D, 0.6D)), 1),
                        layer("left_sclera", LEFT_SCLERA, LayerRole.DECORATION,
                                Transform3.IDENTITY.withTranslation(new Vec3(0.0D, 0.0D, 0.6D)), 4),
                        layer("left_pupil", LEFT_PUPIL, LayerRole.PUPIL,
                                Transform3.IDENTITY.withTranslation(new Vec3(0.01D, 0.0D, 0.62D)), 1)),
                List.of(), "fingerprint");
    }

    private FaceRigDefinition faceRootLayerRig() {
        return new FaceRigDefinition(FaceRigId.parse("jdh:john_father"),
                new BoneHierarchy(List.of(BoneDefinition.root(FACE_ROOT))),
                List.of(
                        layer("sclera_left", FACE_ROOT, LayerRole.DECORATION,
                                Transform3.IDENTITY.withTranslation(new Vec3(0.0D, 0.0D, 0.6D)), 4),
                        layer("pupil_left", FACE_ROOT, LayerRole.PUPIL,
                                Transform3.IDENTITY.withTranslation(new Vec3(0.11D, 0.0D, 0.62D)), 1),
                        layer("sclera_right", FACE_ROOT, LayerRole.DECORATION,
                                Transform3.IDENTITY.withTranslation(new Vec3(0.0D, 0.0D, 0.6D)), 4),
                        layer("pupil_right", FACE_ROOT, LayerRole.PUPIL,
                                Transform3.IDENTITY.withTranslation(new Vec3(-0.11D, 0.0D, 0.62D)), 1)),
                List.of(), "fingerprint");
    }

    private BoneDefinition bone(BoneId id, BoneId parent) {
        return new BoneDefinition(id, Optional.of(parent), Transform3.IDENTITY);
    }

    private boolean hasLayer(List<ProjectedFaceLayer> projected, String id) {
        return projected.stream().anyMatch(layer -> layer.command().layerId().equals(FaceLayerId.parse(id)));
    }

    private ProjectedFaceLayer layer(List<ProjectedFaceLayer> projected, String id) {
        return projected.stream()
                .filter(layer -> layer.command().layerId().equals(FaceLayerId.parse(id)))
                .findFirst()
                .orElseThrow();
    }

    private FaceLayerDefinition layer(String id, BoneId bone, LayerRole role, Transform3 placement, int width) {
        return new FaceLayerDefinition(FaceLayerId.parse(id), bone, FaceTextureId.parse("jdh:face/" + id),
                role, role == LayerRole.PUPIL ? 30 : 20, placement, new IntRect(0, 0, width, 1),
                Optional.empty(), true);
    }
}
