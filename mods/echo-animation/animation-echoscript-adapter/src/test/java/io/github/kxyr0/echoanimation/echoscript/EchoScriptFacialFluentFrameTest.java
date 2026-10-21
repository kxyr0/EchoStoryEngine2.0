package io.github.kxyr0.echoanimation.echoscript;

import io.github.kxyr0.echoanimation.testsupport.TestSources;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class EchoScriptFacialFluentFrameTest {
    private final EchoScriptAnimationCompiler compiler = new EchoScriptAnimationCompiler();

    @Test
    void facialFramePositionDoesNotRequireRotationModifier() {
        AnimationCompileResult result = compiler.compile("sleep.face-animation.es",
                TestSources.read("/ee1/fixtures/EchoScriptFacialFluentFrameTest-1.es"));

        Assertions.assertTrue(result.successful(), result.diagnostics().toString());
        var transform = result.facialAnimation().orElseThrow()
                .surface("3d", FaceRigId.parse("jdh:sasha"))
                .orElseThrow()
                .enter()
                .tracks()
                .get(0)
                .keyframes()
                .get(1)
                .transform();
        Assertions.assertEquals(0.07D, transform.translation().y(), 0.0001D);
        Assertions.assertEquals(0.0D, transform.rotationZDegrees(), 0.0001D);
    }
}
