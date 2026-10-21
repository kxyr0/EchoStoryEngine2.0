package io.github.kxyr0.echoanimation.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.id.FaceTextureId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.validation.FaceRigValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class FaceLayerPairValidatorTest {
    private static final BoneId FACE_ROOT = BoneId.parse("face_root");
    private static final FaceRigId RIG_ID = FaceRigId.parse("jdh:john_father");

    @Test
    void authoredPupilLayersRequireMatchingScleraLayers() {
        Assertions.assertAll(
                () -> assertRejected(layer("pupil_left", LayerRole.PUPIL, 30)),
                () -> assertRejected(layer("pupil_right", LayerRole.PUPIL, 30)),
                () -> assertRejected(layer("left_pupil", LayerRole.PUPIL, 30)),
                () -> assertRejected(layer("right_pupil", LayerRole.PUPIL, 30)));
    }

    @Test
    void matchingScleraLayerCannotBePupilRole() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> validate(
                layer("sclera_left", LayerRole.PUPIL, 20),
                layer("pupil_left", LayerRole.PUPIL, 30)));
    }

    @Test
    void currentAuthoredFaceRootPupilScleraPairsAreAccepted() {
        Assertions.assertDoesNotThrow(() -> validate(
                layer("sclera_left", LayerRole.DECORATION, 20),
                layer("pupil_left", LayerRole.PUPIL, 30),
                layer("sclera_right", LayerRole.DECORATION, 21),
                layer("pupil_right", LayerRole.PUPIL, 31)));
    }

    private void assertRejected(FaceLayerDefinition layer) {
        Assertions.assertThrows(IllegalArgumentException.class, () -> validate(layer));
    }

    private void validate(FaceLayerDefinition... layers) {
        new FaceRigValidator().validate(new FaceRigDefinition(RIG_ID,
                new BoneHierarchy(List.of(BoneDefinition.root(FACE_ROOT))), List.of(layers), List.of(),
                "fingerprint"));
    }

    private FaceLayerDefinition layer(String id, LayerRole role, int order) {
        return new FaceLayerDefinition(FaceLayerId.parse(id), FACE_ROOT, FaceTextureId.parse("jdh:face/" + id),
                role, order, Transform3.IDENTITY, new IntRect(0, 0, 1, 1), Optional.empty(), true);
    }
}
