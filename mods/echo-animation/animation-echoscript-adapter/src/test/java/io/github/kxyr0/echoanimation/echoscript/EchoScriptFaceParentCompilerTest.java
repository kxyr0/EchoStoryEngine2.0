package io.github.kxyr0.echoanimation.echoscript;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class EchoScriptFaceParentCompilerTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void canonicalPupilWithoutMatchingScleraIsRejectedByEchoScriptCompiler() {
        Assertions.assertAll(
                () -> assertFaceRigRejected("""
                        Bone("face_root"),
                        Bone("left_eye_root", parent: "face_root"),
                        Bone("left_pupil", parent: "left_eye_root")
                        """),
                () -> assertFaceRigRejected("""
                        Bone("face_root"),
                        Bone("right_eye_root", parent: "face_root"),
                        Bone("right_pupil", parent: "right_eye_root")
                        """));
    }

    @Test
    void canonicalPupilSiblingOfScleraIsRejectedByEchoScriptCompiler() {
        Assertions.assertAll(
                () -> assertFaceRigRejected("""
                        Bone("face_root"),
                        Bone("left_eye_root", parent: "face_root"),
                        Bone("left_sclera", parent: "left_eye_root"),
                        Bone("left_pupil", parent: "left_eye_root")
                        """),
                () -> assertFaceRigRejected("""
                        Bone("face_root"),
                        Bone("right_eye_root", parent: "face_root"),
                        Bone("right_sclera", parent: "right_eye_root"),
                        Bone("right_pupil", parent: "right_eye_root")
                        """));
    }

    private void assertFaceRigRejected(String bones) {
        AnimationCompileResult result = compiler.compile("bad.face-rig.es", """
                namespace JDH.Animation;

                class BadRig : FaceRigResource
                {
                    public override FaceRigDef Build()
                    {
                        return FaceRig("jdh:bad", bones: [
                            %s
                        ]);
                    }
                }
                """.formatted(bones));
        Assertions.assertFalse(result.successful(), result.diagnostics().toString());
        Assertions.assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.error()));
    }
}
