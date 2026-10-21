package io.github.kxyr0.echoanimation.echoscript;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class EchoScriptFaceLayerPairCompilerTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void authoredPupilLayerWithoutMatchingScleraLayerIsRejected() {
        Assertions.assertAll(
                () -> assertFaceRigRejected("""
                        Layer("pupil_left", bone: "face_root", texture: "jdh:face/pupil_left",
                            role: "PUPIL", order: 30, x: 0, y: 0, w: 1, h: 2)
                        """),
                () -> assertFaceRigRejected("""
                        Layer("pupil_right", bone: "face_root", texture: "jdh:face/pupil_right",
                            role: "PUPIL", order: 30, x: 0, y: 0, w: 1, h: 2)
                        """));
    }

    @Test
    void currentAuthoredPupilScleraPairsCompileOnFaceRoot() {
        AnimationCompileResult result = compiler.compile("current.face-rig.es", """
                namespace JDH.Animation;

                class CurrentRig : FaceRigResource
                {
                    public override FaceRigDef Build()
                    {
                        return FaceRig("jdh:current",
                            bones: [Bone("face_root")],
                            layers: [
                                Layer("sclera_left", bone: "face_root", texture: "jdh:face/sclera_left",
                                    role: "DECORATION", order: 20, x: 0, y: 0, w: 2, h: 2),
                                Layer("pupil_left", bone: "face_root", texture: "jdh:face/pupil_left",
                                    role: "PUPIL", order: 30, x: 0, y: 0, w: 1, h: 2)
                            ]);
                    }
                }
                """);
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
    }

    private void assertFaceRigRejected(String layer) {
        AnimationCompileResult result = compiler.compile("bad.face-rig.es", """
                namespace JDH.Animation;

                class BadRig : FaceRigResource
                {
                    public override FaceRigDef Build()
                    {
                        return FaceRig("jdh:bad",
                            bones: [Bone("face_root")],
                            layers: [%s]);
                    }
                }
                """.formatted(layer));
        Assertions.assertFalse(result.successful(), result.diagnostics().toString());
        Assertions.assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.error()));
    }
}
