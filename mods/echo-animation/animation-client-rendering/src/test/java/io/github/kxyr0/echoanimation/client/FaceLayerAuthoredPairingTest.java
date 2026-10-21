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

final class FaceLayerAuthoredPairingTest {
    private static final BoneId FACE_ROOT = BoneId.parse("face_root");

    @Test
    void pupilLeftLayerIsClampedByTranslatedScleraLeftLayer() {
        var left = project(authoredStyleRig(-0.125D, -0.09D)).stream()
                .filter(layer -> layer.command().layerId().equals(FaceLayerId.parse("pupil_left")))
                .findFirst()
                .orElseThrow();

        Assertions.assertEquals(-0.09375D, left.transform().translation().x(), 0.0001D);
        Assertions.assertEquals(0.0D, left.transform().translation().y(), 0.0001D);
        Assertions.assertEquals(1, left.command().atlasRect().width());
        Assertions.assertEquals(2, left.command().atlasRect().height());
    }

    @Test
    void pupilRightLayerIsClampedByTranslatedScleraRightLayer() {
        var right = project(authoredStyleRig(0.125D, 0.09D)).stream()
                .filter(layer -> layer.command().layerId().equals(FaceLayerId.parse("pupil_right")))
                .findFirst()
                .orElseThrow();

        Assertions.assertEquals(0.09375D, right.transform().translation().x(), 0.0001D);
        Assertions.assertEquals(0.0D, right.transform().translation().y(), 0.0001D);
        Assertions.assertEquals(1, right.command().atlasRect().width());
        Assertions.assertEquals(2, right.command().atlasRect().height());
    }

    @Test
    void pupilLeftLayerFullyOutsidePairedScleraIsClamped() {
        var projected = project(authoredStyleRig(-0.125D, 0.25D, 0.0D));

        Assertions.assertEquals(-0.09375D, layer(projected, "pupil_left").transform().translation().x(), 0.0001D);
    }

    @Test
    void pupilRightLayerFullyOutsidePairedScleraIsClamped() {
        var projected = project(authoredStyleRig(0.125D, -0.25D, 0.0D));

        Assertions.assertEquals(0.09375D, layer(projected, "pupil_right").transform().translation().x(), 0.0001D);
    }

    private List<ProjectedFaceLayer> project(FaceRigDefinition rig) {
        return new FaceLayerPoseProjector().project(rig, new BonePose(Map.of()), Set.of());
    }

    private FaceRigDefinition authoredStyleRig(double scleraX, double pupilX) {
        return authoredStyleRig(scleraX, pupilX, 0.11D);
    }

    private FaceRigDefinition authoredStyleRig(double scleraX, double pupilX, double pupilY) {
        String side = scleraX < 0.0D ? "left" : "right";
        return new FaceRigDefinition(FaceRigId.parse("jdh:john_father"),
                new BoneHierarchy(List.of(BoneDefinition.root(FACE_ROOT))),
                List.of(
                        layer("sclera_" + side, LayerRole.DECORATION, placement(scleraX, 0.0D),
                                new IntRect(0, 0, 2, 2)),
                        layer("pupil_" + side, LayerRole.PUPIL, placement(pupilX, pupilY),
                                new IntRect(0, 0, 1, 2))),
                List.of(), "fingerprint");
    }

    private ProjectedFaceLayer layer(List<ProjectedFaceLayer> projected, String id) {
        return projected.stream()
                .filter(layer -> layer.command().layerId().equals(FaceLayerId.parse(id)))
                .findFirst()
                .orElseThrow();
    }

    private Transform3 placement(double placeX, double placeY) {
        return Transform3.IDENTITY.withTranslation(new Vec3(placeX, placeY, 0.62D));
    }

    private FaceLayerDefinition layer(
            String id,
            LayerRole role,
            Transform3 placement,
            IntRect atlas
    ) {
        return new FaceLayerDefinition(FaceLayerId.parse(id), FACE_ROOT, FaceTextureId.parse("jdh:face/" + id),
                role, role == LayerRole.PUPIL ? 30 : 20, placement, atlas, Optional.empty(), true);
    }
}
