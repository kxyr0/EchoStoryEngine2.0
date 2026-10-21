package io.github.kxyr0.echoanimation.echoscript;

import io.github.kxyr0.echoanimation.testsupport.TestSources;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class EchoScriptFacialAnimationHelperMethodTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void facialAnimationCanUseSiblingSurfaceClasses() {
        AnimationCompileResult result = compiler.compile("look_left_helpers.face-animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptFacialAnimationHelperMethodTest-1.es"));

        var animation = result.facialAnimation().orElseThrow();

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        Assertions.assertEquals(2, animation.surfaces().size());
        Assertions.assertEquals("3d", animation.surfaces().get(0).name());
        Assertions.assertEquals("portrait", animation.surfaces().get(1).name());
        Assertions.assertTrue(animation.surface("3d", FaceRigId.parse("jdh:john_father")).isPresent());
        Assertions.assertTrue(animation.surfaces().get(1).rigId().isEmpty());
        Assertions.assertEquals(-0.031D, animation.surfaces().get(0).enter()
                .tracks().get(0).keyframes().get(1).transform().translation().x(), 0.0001D);
        Assertions.assertEquals(-0.020D, animation.surfaces().get(1).enter()
                .tracks().get(0).keyframes().get(1).transform().translation().x(), 0.0001D);
    }
}
