package io.github.kxyr0.echoanimation.echoscript;

import io.github.kxyr0.echoanimation.api.math.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class EchoScriptRequirementMatrixTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void malformedSourceProducesNoDefinition() {
        AnimationCompileResult result = compiler.compile("bad.face-rig.es", "namespace JDH.Animation class");
        Assertions.assertFalse(result.successful());
        Assertions.assertTrue(result.faceRig().isEmpty());
    }

    @Test
    void unknownIntrinsicRejected() {
        AnimationCompileResult result = compiler.compile("bad.face-rig.es", """
                namespace JDH.Animation;
                class Bad : FaceRigResource
                {
                    public override FaceRigDef Build()
                    {
                        return FacePose("jdh:john_father/neutral", rig: "jdh:john_father");
                    }
                }
                """);
        Assertions.assertFalse(result.successful());
    }

    @Test
    void formattingDoesNotAlterSemanticFingerprint() {
        String compact = "namespace JDH.Animation; class A : FaceRigResource { public override FaceRigDef Build()"
                + " { return FaceRig(\"jdh:john_father\", bones: [Bone(\"face_root\")]); } }";
        String spaced = """
                namespace JDH.Animation;
                class B : FaceRigResource
                {
                    public override FaceRigDef Build()
                    {
                        return FaceRig("jdh:john_father", bones: [Bone("face_root")]);
                    }
                }
                """;
        String first = compiler.compile("a.face-rig.es", compact).faceRig().orElseThrow().semanticFingerprint();
        String second = compiler.compile("b.face-rig.es", spaced).faceRig().orElseThrow().semanticFingerprint();
        Assertions.assertEquals(first, second);
    }

    @Test
    void poseBoneDeltaCannotChangePivot() {
        AnimationCompileResult result = compiler.compile("pose.face-pose.es", """
                namespace JDH.Animation;
                class Pose : FacePoseResource
                {
                    public override FacePoseDef Build()
                    {
                        return FacePose("jdh:john_father/neutral", rig: "jdh:john_father",
                            bones: [BoneDelta("face_root", x: 10, pivotX: 9000)]);
                    }
                }
                """);
        Assertions.assertEquals(Vec3.ZERO, result.facePose().orElseThrow()
                .boneDeltas()
                .values()
                .iterator()
                .next()
                .pivot());
    }

    @Test
    void facialAnimationReturnToBaseEndCompiles() {
        AnimationCompileResult result = compiler.compile("clip.face-animation.es", animation("RETURN_TO_BASE",
                "LINEAR"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("RETURN_TO_BASE", result.facialAnimation().orElseThrow().endBehavior().name());
    }

    @Test
    void stepInterpolationCompiles() {
        AnimationCompileResult result = compiler.compile("clip.face-animation.es", animation("HOLD", "STEP"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
    }

    @Test
    void duplicateFrameTimeFailsBinding() {
        AnimationCompileResult result = compiler.compile("bad.face-animation.es", """
                namespace JDH.Animation;
                class Bad : FacialAnimationResource
                {
                    public override FacialAnimationDef Build()
                    {
                        return FacialAnimation("jdh:shared/blink", rig: "jdh:john_father",
                            durationMillis: 1000, tracks: [Track("face_root",
                                    frames: [Frame(0), Frame(0)])]);
                    }
                }
                """);
        Assertions.assertFalse(result.successful());
    }

    @Test
    void invalidClipRectFailsBinding() {
        AnimationCompileResult result = compiler.compile("bad.face-rig.es", """
                namespace JDH.Animation;
                class Bad : FaceRigResource
                {
                    public override FaceRigDef Build()
                    {
                        return FaceRig("jdh:john_father",
                            bones: [Bone("face_root")],
                            layers: [Layer("pupil_left", bone: "face_root", texture: "jdh:face/pupil",
                                    clipX: 60, clipY: 60, clipW: 8, clipH: 8)]);
                    }
                }
                """);
        Assertions.assertFalse(result.successful());
    }

    private String animation(String end, String interpolation) {
        return """
                namespace JDH.Animation;
                class Clip : FacialAnimationResource
                {
                    public override FacialAnimationDef Build()
                    {
                        return FacialAnimation("jdh:shared/blink", rig: "jdh:john_father",
                            durationMillis: 1000, end: "%s", tracks: [Track("face_root",
                                    frames: [Frame(0), Frame(1000, y: 220, interpolation: "%s")])]);
                    }
                }
                """.formatted(end, interpolation);
    }
}
