package io.github.kxyr0.echoanimation.echoscript;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class EchoScriptBodyClipFluentFrameTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void bodyClipCanUseFluentFrameTransforms() {
        AnimationCompileResult result = compiler.compile("fluent_body.animation-clip.es", """
                namespace JDH.Animation;

                class FluentBodyClip : AnimationClipResource
                {
                    public override AnimationClipDef Build()
                    {
                        return AnimationClip("jdh:fluent_body", rig: "jdh:humanoid",
                            tracks: [Track("body",
                                    frames: [
                                        Frame("0.0").Position(0, "12.0", 0).Rotation(0, 0, 0),
                                        Frame("1.0").Position(0, "9.5", 0)
                                                .Rotation("-28.0", 0, "12.0").Scale(1, 1, 1)
                                    ])]);
                    }
                }
                """);

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        var keyframe = result.animationClip().orElseThrow().tracks().get(0).keyframes().get(1);
        Assertions.assertEquals(9.5D, keyframe.translation().orElseThrow().y(), 0.0001D);
        Assertions.assertEquals(-28.0D, keyframe.rotationDegrees().orElseThrow().x(), 0.0001D);
        Assertions.assertEquals(12.0D, keyframe.rotationDegrees().orElseThrow().z(), 0.0001D);
        Assertions.assertEquals(1.0D, keyframe.scale().orElseThrow().x(), 0.0001D);
    }

    @Test
    void bodyClipCanUseExactMatrixFrame() {
        AnimationCompileResult result = compiler.compile("matrix_body.animation-clip.es", """
                namespace JDH.Animation;

                class MatrixBodyClip : AnimationClipResource
                {
                    public override AnimationClipDef Build()
                    {
                        return AnimationClip("jdh:matrix_body", rig: "jdh:therig_2_6",
                            tracks: [Track("body_upper",
                                    frames: [
                                        Frame("0.0").Matrix(
                                            "1.0", "0.25", "0.0", "2.0",
                                            "0.0", "1.0", "0.0", "3.0",
                                            "0.0", "0.0", "1.0", "0.0",
                                            "0.0", "0.0", "0.0", "1.0"),
                                        Frame("1.0").Matrix(
                                            "1.0", "0.5", "0.0", "4.0",
                                            "0.0", "1.0", "0.0", "6.0",
                                            "0.0", "0.0", "1.0", "0.0",
                                            "0.0", "0.0", "0.0", "1.0")
                                    ])]);
                    }
                }
                """);

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        var matrix = result.animationClip().orElseThrow().tracks().get(0).keyframes().get(0)
                .localMatrix().orElseThrow();
        Assertions.assertEquals(0.25D, matrix.value(0, 1), 0.0001D);
        Assertions.assertEquals(2.0D, matrix.value(0, 3), 0.0001D);
    }
}
