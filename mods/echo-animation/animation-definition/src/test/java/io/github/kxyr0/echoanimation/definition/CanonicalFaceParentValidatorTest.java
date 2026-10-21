package io.github.kxyr0.echoanimation.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.definition.validation.FaceRigValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class CanonicalFaceParentValidatorTest {
    private static final FaceRigId RIG_ID = FaceRigId.parse("jdh:john_father");

    @Test
    void canonicalPupilsMustBeChildrenOfScleraWhenEyeBonesExist() {
        Assertions.assertAll(
                () -> assertRejected("left_eye_root", "left_sclera", "left_pupil"),
                () -> assertRejected("right_eye_root", "right_sclera", "right_pupil"));
    }

    @Test
    void canonicalPupilsUnderScleraAreAccepted() {
        Assertions.assertAll(
                () -> new FaceRigValidator().validate(rig("left_eye_root", "left_sclera", "left_pupil",
                        "left_sclera")),
                () -> new FaceRigValidator().validate(rig("right_eye_root", "right_sclera", "right_pupil",
                        "right_sclera")));
    }

    @Test
    void canonicalPupilsRequireScleraBones() {
        Assertions.assertAll(
                () -> assertMissingParentRejected("left_eye_root", "left_pupil"),
                () -> assertMissingParentRejected("right_eye_root", "right_pupil"));
    }

    private void assertRejected(String eyeRoot, String sclera, String pupil) {
        FaceRigDefinition rig = rig(eyeRoot, sclera, pupil, eyeRoot);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FaceRigValidator().validate(rig));
    }

    private void assertMissingParentRejected(String eyeRoot, String pupil) {
        List<BoneDefinition> bones = List.of(
                BoneDefinition.root(BoneId.parse("face_root")),
                bone(eyeRoot, "face_root"),
                bone(pupil, eyeRoot));
        FaceRigDefinition rig = new FaceRigDefinition(RIG_ID, new BoneHierarchy(bones), List.of(), List.of(),
                "fingerprint");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FaceRigValidator().validate(rig));
    }

    private FaceRigDefinition rig(String eyeRoot, String sclera, String pupil, String pupilParent) {
        List<BoneDefinition> bones = List.of(
                BoneDefinition.root(BoneId.parse("face_root")),
                bone(eyeRoot, "face_root"),
                bone(sclera, eyeRoot),
                bone(pupil, pupilParent));
        return new FaceRigDefinition(RIG_ID, new BoneHierarchy(bones), List.of(), List.of(), "fingerprint");
    }

    private BoneDefinition bone(String id, String parent) {
        return new BoneDefinition(BoneId.parse(id), Optional.of(BoneId.parse(parent)), Transform3.IDENTITY);
    }
}
