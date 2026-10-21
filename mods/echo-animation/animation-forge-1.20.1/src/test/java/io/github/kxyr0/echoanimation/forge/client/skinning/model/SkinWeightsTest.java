package io.github.kxyr0.echoanimation.forge.client.skinning.model;

import io.github.kxyr0.echoanimation.forge.client.skinning.model.weights.SkinWeights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SkinWeightsTest {
    @Test
    void normalizesWeights() {
        SkinWeights skin = SkinWeights.of(new int[]{2, 3}, new float[]{2.0F, 6.0F}, 0);

        assertEquals(0.75F, skin.weight(0), 0.0001F);
        assertEquals(0.25F, skin.weight(1), 0.0001F);
    }

    @Test
    void keepsFourStrongestInfluences() {
        SkinWeights skin = SkinWeights.of(new int[]{0, 1, 2, 3, 4},
                new float[]{0.1F, 0.4F, 0.2F, 0.3F, 0.5F}, 0);

        assertTrue(skin.truncated());
        assertEquals(4, skin.index(0));
        assertEquals(1, skin.index(1));
        assertEquals(3, skin.index(2));
        assertEquals(2, skin.index(3));
        assertEquals(1.0F, skin.weight(0) + skin.weight(1) + skin.weight(2) + skin.weight(3), 0.0001F);
    }

    @Test
    void emptyInfluencesFallBackToRoot() {
        SkinWeights skin = SkinWeights.of(new int[0], new float[0], 7);

        assertTrue(skin.fallback());
        assertEquals(7, skin.index(0));
        assertEquals(1.0F, skin.weight(0));
    }
}
