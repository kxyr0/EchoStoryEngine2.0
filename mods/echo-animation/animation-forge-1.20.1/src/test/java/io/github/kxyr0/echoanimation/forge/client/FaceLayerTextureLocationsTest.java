package io.github.kxyr0.echoanimation.forge.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class FaceLayerTextureLocationsTest {
    @Test
    void normalizesRepeatedSlashesInAuthoredTexturePath() {
        ResourceLocation texture = FaceLayerTextureLocations.clientTexture(
                "jdh:echo_animation/faces/player//sclera_left");

        Assertions.assertEquals(ResourceLocation.fromNamespaceAndPath("jdh",
                "textures/echo_animation/faces/player/sclera_left.png"), texture);
    }
}
