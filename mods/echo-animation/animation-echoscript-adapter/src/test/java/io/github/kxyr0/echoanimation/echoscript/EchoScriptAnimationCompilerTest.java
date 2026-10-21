package io.github.kxyr0.echoanimation.echoscript;

import io.github.kxyr0.echoanimation.testsupport.TestSources;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class EchoScriptAnimationCompilerTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void validFaceRigResourceCompiles() {
        AnimationCompileResult result = compiler.compile("rig.face-rig.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-1.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("jdh:john_father", result.faceRig().orElseThrow().id().toString());
        Assertions.assertEquals(0.63D, result.faceRig().orElseThrow().layers().get(0)
                .placement()
                .translation()
                .z(), 0.0001D);
    }

    @Test
    void scleraParentedPupilFaceRigCompiles() {
        AnimationCompileResult result = compiler.compile("john_father.face-rig.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-2.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        assertParent(result, "left_pupil", "left_sclera");
        assertParent(result, "right_pupil", "right_sclera");
    }

    @Test
    void validFacePoseResourceCompilesWithRussianComment() {
        AnimationCompileResult result = compiler.compile("pose.face-pose.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-3.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("jdh:john_father/neutral", result.facePose().orElseThrow().id().toString());
    }

    @Test
    void validFacialAnimationResourceCompiles() {
        AnimationCompileResult result = compiler.compile("blink.face-animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-4.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("jdh:shared/blink", result.facialAnimation().orElseThrow().id().toString());
    }

    @Test
    void facialAnimationCanSuppressIdleBlink() {
        AnimationCompileResult result = compiler.compile("sleep.face-animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-5.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertTrue(result.facialAnimation().orElseThrow().suppressIdleBlink());
    }

    @Test
    void riglessFacialAnimationCompilesAsUniversalClip() {
        AnimationCompileResult result = compiler.compile("look_left.face-animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-6.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertTrue(result.facialAnimation().orElseThrow().rigId().isEmpty());
    }

    @Test
    void facialAnimationCanUseTickTiming() {
        AnimationCompileResult result = compiler.compile("look_left_ticks.face-animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-7.es"));

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals(1.2D, result.facialAnimation().orElseThrow().durationSeconds(), 0.0001D);
        Assertions.assertEquals(0.3D, result.facialAnimation().orElseThrow()
                .tracks().get(0).keyframes().get(1).timeSeconds(), 0.0001D);
    }

    @Test
    void facialAnimationCanUseFluentFrameTransforms() {
        AnimationCompileResult result = compiler.compile("fluent.face-animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-8.es"));

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        var transform = result.facialAnimation().orElseThrow().tracks().get(0).keyframes().get(1).transform();
        Assertions.assertEquals(-0.031D, transform.translation().x(), 0.0001D);
        Assertions.assertEquals(0.004D, transform.translation().y(), 0.0001D);
        Assertions.assertEquals(-18.0D, transform.rotationZDegrees(), 0.0001D);
        Assertions.assertEquals(1.25D, transform.scale().x(), 0.0001D);
    }

    @Test
    void facialAnimationCanUseSurfacePhases() {
        AnimationCompileResult result = compiler.compile("look_left_split.face-animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-9.es"));

        var animation = result.facialAnimation().orElseThrow();

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals(2, animation.surfaces().size());
        Assertions.assertTrue(animation.surface("3d", FaceRigId.parse("jdh:john_father")).isPresent());
        Assertions.assertTrue(animation.surface("portrait", FaceRigId.parse("jdh:any")).isPresent());
        Assertions.assertEquals(0.2D, animation.surfaces().get(0).enter().durationSeconds(), 0.0001D);
    }

    @Test
    void validSegmentedHumanoidRigResourceCompiles() {
        AnimationCompileResult result = compiler.compile("humanoid.animation-rig.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-10.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("jdh:humanoid", result.animationRig().orElseThrow().id().toString());
        Assertions.assertEquals(14, result.animationRig().orElseThrow().bones().size());
    }

    @Test
    void validNormalizedBodyClipResourceCompiles() {
        AnimationCompileResult result = compiler.compile("wave.animation-clip.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-11.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("jdh:humanoid_test_wave", result.animationClip().orElseThrow().id().toString());
        Assertions.assertEquals(2, result.animationClip().orElseThrow().schemaVersion());
        Assertions.assertEquals(AnimationLoopSeam.SMOOTH, result.animationClip().orElseThrow().loopSeam());
        Assertions.assertEquals(1.2D, result.animationClip().orElseThrow().authoredSpeedBlocksPerSecond(), 0.0001D);
        Assertions.assertEquals(0.5D, result.animationClip().orElseThrow().tracks().get(0)
                .keyframes().get(1).progress(), 0.0001D);
        Assertions.assertEquals(InterpolationKind.SMOOTH, result.animationClip().orElseThrow().tracks().get(0)
                .keyframes().get(1).interpolation());
    }

    @Test
    void validAnimationProfileResourceCompiles() {
        AnimationCompileResult result = compiler.compile("standard.animation-profile.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-12.es"));
        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals("jdh:standard_humanoid", result.animationProfile().orElseThrow().id().toString());
        Assertions.assertEquals("body/idle", result.animationProfile().orElseThrow().initialStates()
                .values().iterator().next().toString());
    }

    @Test
    void unsupportedFieldProducesNoDefinition() {
        AnimationCompileResult result = compiler.compile("bad.face-rig.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-13.es"));
        Assertions.assertFalse(result.successful());
        Assertions.assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.error()));
    }

    @Test
    void diagnosticsIncludeRussianSourceLocation() {
        AnimationCompileResult result = compiler.compile("jdh:echo_animation/animations/bad.animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptAnimationCompilerTest-14.es"));

        String message = result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.code().equals("EANIM-ES-UNSUPPORTED-FIELD"))
                .findFirst()
                .orElseThrow()
                .message();
        Assertions.assertTrue(message.contains("Файл: data/jdh/echo_animation/animations/bad.animation.es"), message);
        Assertions.assertTrue(message.contains("Строка:"), message);
        Assertions.assertTrue(message.contains("Неподдерживаемое поле: unexpected"), message);
        Assertions.assertFalse(message.contains("Unsupported field"), message);
    }

    private void assertParent(AnimationCompileResult result, String child, String parent) {
        var hierarchy = result.faceRig().orElseThrow().hierarchy();
        var bone = hierarchy.find(BoneId.parse(child)).orElseThrow();
        Assertions.assertEquals(BoneId.parse(parent), bone.parentId().orElseThrow());
    }

}
