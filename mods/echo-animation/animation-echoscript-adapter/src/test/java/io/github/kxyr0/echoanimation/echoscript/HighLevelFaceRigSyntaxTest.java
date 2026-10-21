package io.github.kxyr0.echoanimation.echoscript;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.definition.resource.FaceLayerDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class HighLevelFaceRigSyntaxTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void minimalFaceCreatesRootBone() {
        AnimationCompileResult result = compile("""
                face Test("test:test")
                {
                }
                """);

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("test:test", result.faceRig().orElseThrow().id().toString());
        Assertions.assertTrue(result.faceRig().orElseThrow().hierarchy()
                .find(BoneId.parse("face_root")).orElseThrow().parentId().isEmpty());
    }

    @Test
    void compactEyePartsUseConventions() {
        AnimationCompileResult result = compile("""
                face Test("test:test")
                {
                    Eyes
                    {
                        Sclera Left;
                        Pupil Left;
                    }
                }
                """);

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        assertParent(result, "left_eye_root", "face_root");
        assertParent(result, "left_sclera", "left_eye_root");
        assertParent(result, "left_pupil", "left_sclera");
        FaceLayerDefinition pupil = layer(result, "pupil_left");
        Assertions.assertEquals("test:echo_animation/faces/test/pupil_left", pupil.textureId().toString());
        Assertions.assertEquals(LayerRole.PUPIL, pupil.role());
        Assertions.assertEquals(15, pupil.drawOrder());
        Assertions.assertEquals(1, pupil.atlasRect().width());
        Assertions.assertEquals(2, pupil.atlasRect().height());
    }

    @Test
    void targetJohnFatherSyntaxCompiles() {
        AnimationCompileResult result = compile(johnFatherRig());

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        assertParent(result, "upper_eyelid_left", "upper_eyelids");
        assertParent(result, "lower_eyelid_right", "lower_eyelids");
        Assertions.assertEquals(11, result.faceRig().orElseThrow().layers().size());
        Assertions.assertEquals(-0.125D, boneX(result, "brows_left"), 0.0001D);
        Assertions.assertEquals(-0.03D, boneY(result, "upper_eyelids"), 0.0001D);
        Assertions.assertEquals(-0.125D, boneX(result, "upper_eyelid_left"), 0.0001D);
        Assertions.assertEquals(0.0D, layer(result, "brow_left").placement().translation().x(), 0.0001D);
        Assertions.assertEquals(8, layer(result, "upper_eyelid_left").atlasRect().x());
        Assertions.assertEquals(4, layer(result, "upper_eyelid_left").atlasRect().width());
        assertBlinkTiming(result);
    }

    private void assertBlinkTiming(AnimationCompileResult result) {
        var control = result.faceRig().orElseThrow().controls().get(0);
        Assertions.assertEquals(0.035D, control.blinkUpperTravel(), 0.0001D);
        Assertions.assertEquals(-0.035D, control.blinkLowerTravel(), 0.0001D);
        Assertions.assertEquals(80, control.blinkPeriodTicks());
        Assertions.assertEquals(3, control.blinkCloseTicks());
        Assertions.assertEquals(2, control.blinkHoldTicks());
        Assertions.assertEquals(4, control.blinkOpenTicks());
        Assertions.assertTrue(control.idleBlink());
    }

    private String johnFatherRig() {
        return """
                namespace JDH.Animation.Chapter1.Family;

                face JohnFather("jdh:john_father")
                {
                    Eyes
                    {
                        Sclera Left  { Position(-0.125, 0, 0); }
                        Sclera Right { Position( 0.125, 0, 0); }
                        Pupil Left   { Position(-0.09, 0, 0.01); }
                        Pupil Right  { Position( 0.09, 0, 0.01); }
                        Eyelid Upper { Position(0.0011, -0.03, 0.02); TextureArea(8, 13, 8, 1); }
                        Eyelid Lower { Position(0, 0.095, 0.02); TextureArea(8, 12, 8, 1); }
                    }

                    Brows
                    {
                        Pivot(-62, 0);
                        Left  { Position(-0.125, -0.032, 0.03); }
                        Right { Position( 0.124, -0.032, 0.03); }
                    }

                    Bone Glasses;
                    Blink "eye_closure"
                    {
                        Travel(35, -35);
                        Gaze(1, 1);
                        PeriodTicks(80);
                        CloseTicks(3);
                        HoldTicks(2);
                        OpenTicks(4);
                    }
                }
                """;
    }

    @Test
    void textureAndPivotOverridesApply() {
        AnimationCompileResult result = compile("""
                face Test("test:test")
                {
                    Textures("test:custom/faces/test");
                    Eyes { Pupil Left { Texture("damaged_pupil"); Role("DECORATION"); } }
                    Brows { Pivot(4000, 1000); Left; Right { Pivot(2000, 500); } }
                }
                """);

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("test:custom/faces/test/damaged_pupil", layer(result, "pupil_left")
                .textureId().toString());
        Assertions.assertEquals(LayerRole.DECORATION, layer(result, "pupil_left").role());

        AnimationCompileResult brows = compile("""
                face Test("test:test")
                {
                    Brows { Pivot(4000, 1000); Left; Right { Pivot(2000, 500); } }
                }
                """);
        Assertions.assertTrue(brows.successful(), brows.diagnostics().toString());
        Assertions.assertEquals(4.0D, bonePivotX(brows, "brows_left"), 0.0001D);
        Assertions.assertEquals(2.0D, bonePivotX(brows, "brows_right"), 0.0001D);
    }

    @Test
    void glassesBoneVisualOverridesApply() {
        AnimationCompileResult result = compile("""
                face Test("test:test")
                {
                    Bone Glasses
                    {
                        Texture("custom_glasses");
                        Size(6, 2);
                        Order(41);
                        Position(0, 0.06, 0.04);
                    }
                }
                """);

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        FaceLayerDefinition glasses = layer(result, "glasses");
        Assertions.assertEquals("test:echo_animation/faces/test/custom_glasses", glasses.textureId().toString());
        Assertions.assertEquals(LayerRole.GLASSES, glasses.role());
        Assertions.assertEquals(41, glasses.drawOrder());
        Assertions.assertEquals(6, glasses.atlasRect().width());
        Assertions.assertEquals(2, glasses.atlasRect().height());
        Assertions.assertEquals(0.06D, boneY(result, "glasses"), 0.0001D);
    }

    @Test
    void invalidSideProducesDiagnostic() {
        AnimationCompileResult result = compile("""
                face Test("test:test")
                {
                    Eyes { Pupil Middle; }
                }
                """);

        Assertions.assertFalse(result.successful());
        Assertions.assertTrue(result.diagnostics().get(0).message().contains("Middle"));
    }

    private AnimationCompileResult compile(String source) {
        return compiler.compile("test.face-rig.es", source);
    }

    private FaceLayerDefinition layer(AnimationCompileResult result, String id) {
        return result.faceRig().orElseThrow().layers().stream()
                .filter(value -> FaceLayerId.parse(id).equals(value.id()))
                .findFirst()
                .orElseThrow();
    }

    private void assertParent(AnimationCompileResult result, String child, String parent) {
        var bone = result.faceRig().orElseThrow().hierarchy().find(BoneId.parse(child)).orElseThrow();
        Assertions.assertEquals(BoneId.parse(parent), bone.parentId().orElseThrow());
    }

    private double bonePivotX(AnimationCompileResult result, String id) {
        return result.faceRig().orElseThrow().hierarchy().find(BoneId.parse(id)).orElseThrow()
                .localTransform().pivot().x();
    }

    private double boneX(AnimationCompileResult result, String id) {
        return result.faceRig().orElseThrow().hierarchy().find(BoneId.parse(id)).orElseThrow()
                .localTransform().translation().x();
    }

    private double boneY(AnimationCompileResult result, String id) {
        return result.faceRig().orElseThrow().hierarchy().find(BoneId.parse(id)).orElseThrow()
                .localTransform().translation().y();
    }
}
