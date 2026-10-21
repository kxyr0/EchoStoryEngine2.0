package io.github.kxyr0.echoanimation.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FacePoseId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.id.FacialAnimationId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationEndBehavior;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.core.pose.BoneTrack;
import io.github.kxyr0.echoanimation.core.pose.Keyframe;
import io.github.kxyr0.echoanimation.core.transform.TransformEvaluator;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FacePoseDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FacialAnimationDefinition;
import io.github.kxyr0.echoanimation.definition.validation.FacePoseValidator;
import io.github.kxyr0.echoanimation.definition.validation.FaceRigValidator;
import io.github.kxyr0.echoanimation.definition.validation.FacialAnimationValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class DefinitionRequirementMatrixTest {
    private final BoneId root = BoneId.parse("face_root");
    private final BoneId child = BoneId.parse("upper_eyelids");
    private final FaceRigId rigId = FaceRigId.parse("jdh:john_father");

    @Test
    void invalidClippingRegionRejected() {
        FaceLayerDefinition bad = layer("upper_eyelids", child, 10, Optional.of(new IntRect(60, 60, 8, 8)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FaceRigValidator().validate(rig(List.of(bad))));
    }

    @Test
    void layerModelsAreImmutable() {
        List<FaceLayerDefinition> layers = new ArrayList<>(List.of(layer("glasses", root, 20, Optional.empty())));
        FaceRigDefinition rig = rig(layers);
        layers.clear();
        Assertions.assertEquals(1, rig.layers().size());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> rig.layers().add(layer("x", root, 1,
                Optional.empty())));
    }

    @Test
    void duplicateLayerIdRejected() {
        FaceLayerDefinition first = layer("glasses", root, 20, Optional.empty());
        FaceLayerDefinition second = layer("glasses", root, 30, Optional.empty());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FaceRigValidator().validate(rig(List.of(first, second))));
    }

    @Test
    void duplicateDrawOrderRejectedWhenLayersOverlap() {
        FaceLayerDefinition first = layer("left_sclera", root, 20, Optional.empty());
        FaceLayerDefinition second = layer("left_pupil", child, 20, Optional.empty());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FaceRigValidator().validate(rig(List.of(first, second))));
    }

    @Test
    void poseStoresOnlyDeltas() {
        FacePoseDefinition pose = pose(Map.of(child, Transform3.IDENTITY), Map.of());
        Assertions.assertEquals(1, pose.boneDeltas().size());
        Assertions.assertFalse(pose.boneDeltas().containsKey(root));
    }

    @Test
    void poseCannotChangeParentBecauseOnlyTargetBoneDeltasAreStored() {
        FacePoseDefinition pose = pose(Map.of(child, Transform3.IDENTITY), Map.of());
        Assertions.assertEquals(Optional.of(root), rig(List.of()).hierarchy().find(child).orElseThrow().parentId());
        Assertions.assertEquals(child, pose.boneDeltas().keySet().iterator().next());
    }

    @Test
    void poseApplicationIsDeterministic() {
        Transform3 delta = Transform3.IDENTITY.withTranslation(new Vec3(0.0D, 1.0D, 0.0D));
        FacePoseDefinition pose = pose(Map.of(child, delta), Map.of());
        var evaluator = new TransformEvaluator();
        var first = evaluator.evaluate(rig(List.of()).hierarchy(), new BonePose(pose.boneDeltas()));
        var second = evaluator.evaluate(rig(List.of()).hierarchy(), new BonePose(pose.boneDeltas()));
        Assertions.assertEquals(first, second);
    }

    @Test
    void poseUnknownBoneRejected() {
        FacePoseDefinition pose = pose(Map.of(BoneId.parse("missing"), Transform3.IDENTITY), Map.of());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FacePoseValidator().validate(rig(List.of()), pose));
    }

    @Test
    void poseRigMismatchRejected() {
        FacePoseDefinition pose = new FacePoseDefinition(FacePoseId.parse("jdh:other/neutral"),
                FaceRigId.parse("jdh:other"), Map.of(), Map.of(), "fingerprint");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FacePoseValidator().validate(rig(List.of()), pose));
    }

    @Test
    void poseControlDeltaOutOfRangeRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> pose(Map.of(), Map.of(FaceControlId.parse("eye_closure"), 2.0D)));
    }

    @Test
    void animationDurationValidationRejectsZero() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> animation(0.0D, List.of(track(root)), AnimationEndBehavior.HOLD));
    }

    @Test
    void unknownBoneTrackRejected() {
        FacialAnimationDefinition clip = animation(1.0D, List.of(track(BoneId.parse("missing"))),
                AnimationEndBehavior.HOLD);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FacialAnimationValidator().validate(rig(List.of()), clip));
    }

    @Test
    void duplicateTrackRejected() {
        FacialAnimationDefinition clip = animation(1.0D, List.of(track(child), track(child)),
                AnimationEndBehavior.HOLD);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new FacialAnimationValidator().validate(rig(List.of()), clip));
    }

    @Test
    void animationCannotAlterHierarchyBecauseOnlyTracksAreStored() {
        FaceRigDefinition rig = rig(List.of());
        new FacialAnimationValidator().validate(rig, animation(1.0D, List.of(track(child)), AnimationEndBehavior.HOLD));
        Assertions.assertEquals(Optional.of(root), rig.hierarchy().find(child).orElseThrow().parentId());
    }

    private FaceRigDefinition rig(List<FaceLayerDefinition> layers) {
        var rootBone = BoneDefinition.root(root);
        var childBone = new BoneDefinition(child, Optional.of(root), Transform3.IDENTITY);
        return new FaceRigDefinition(rigId, new BoneHierarchy(List.of(rootBone, childBone)), layers, List.of(),
                "fingerprint");
    }

    private FacePoseDefinition pose(Map<BoneId, Transform3> bones, Map<FaceControlId, Double> controls) {
        return new FacePoseDefinition(FacePoseId.parse("jdh:john_father/neutral"), rigId, bones, controls,
                "fingerprint");
    }

    private FacialAnimationDefinition animation(
            double duration,
            List<BoneTrack> tracks,
            AnimationEndBehavior end
    ) {
        return new FacialAnimationDefinition(FacialAnimationId.parse("jdh:shared/blink"), Optional.of(rigId),
                duration, end, tracks, List.of(), false, "fingerprint");
    }

    private BoneTrack track(BoneId boneId) {
        return new BoneTrack(boneId, List.of(new Keyframe(0.0D, Transform3.IDENTITY, InterpolationKind.LINEAR)));
    }

    private FaceLayerDefinition layer(String id, BoneId boneId, int order, Optional<IntRect> clip) {
        return new FaceLayerDefinition(FaceLayerId.parse(id), boneId, FaceTextureId.parse("jdh:face/" + id),
                LayerRole.EYELID, order, Transform3.IDENTITY, new IntRect(0, 0, 8, 8), clip, true);
    }
}
