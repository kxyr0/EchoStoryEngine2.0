package io.github.kxyr0.echoanimation.forge.client.dialogue;

import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.forge.client.portrait.PortraitLayerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class DialogueVisualFaceLayerRendererTest {
    @Test
    void portraitPlacementOverridesNeutralBaseAndPreservesAnimationDelta() {
        Transform3 neutral = Transform3.IDENTITY.withTranslation(new Vec3(0.125D, 0.095D, 0.02D));
        Transform3 animated = Transform3.IDENTITY.withTranslation(new Vec3(0.125D, 0.060D, 0.02D));
        PortraitLayerConfig layer = new PortraitLayerConfig(
                0.062D, 0.034D, null, null, 0.5D, 0.5D, null, null, null);

        Transform3 result = DialogueVisualFaceLayerRenderer.overridePlacement(animated, neutral, layer);

        Assertions.assertEquals(0.062D, result.translation().x(), 0.0001D);
        Assertions.assertEquals(-0.001D, result.translation().y(), 0.0001D);
        Assertions.assertEquals(0.5D, result.scale().x(), 0.0001D);
        Assertions.assertEquals(0.5D, result.scale().y(), 0.0001D);
    }

    @Test
    void johnWardrobeVisualUsesBasePlayerRig() {
        Assertions.assertEquals("echo_content_player:player/base",
                DialogueVisualFaceLayerRenderer.rigVisualId(
                        "jdh:echo_wardrobe/skins/combo/john_combo_gnew_cnone_lnone_fclassic_without_glasses"));
    }
}
