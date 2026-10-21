package io.github.kxyr0.echoanimation.api;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class IdentityContractsTest {
    @Test
    void validFaceRigIdIsAccepted() {
        Assertions.assertEquals("jdh:john_father", FaceRigId.parse("jdh:john_father").toString());
    }

    @Test
    void invalidFaceRigIdIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FaceRigId.parse("John Father"));
    }

    @Test
    void validBoneIdIsAccepted() {
        Assertions.assertEquals("face_root", BoneId.parse("face_root").toString());
    }

    @Test
    void validAnimationSubjectIdIsNeutral() {
        Assertions.assertEquals("jdh:john_father", new AnimationSubjectId("jdh:john_father").toString());
    }

    @Test
    void nonFiniteTransformIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new Transform3(Vec3.ZERO, Double.NaN, Vec3.ONE, Vec3.ZERO));
    }
}
