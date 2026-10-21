package io.github.kxyr0.echouianimations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnimationPresetCatalogTest {
    @Test
    void catalogContainsStableBuiltInPresets() {
        assertEquals(7, AnimationPresetCatalog.all().size());
        assertTrue(AnimationPresetCatalog.all().contains(AnimationPresetCatalog.FADE_IN));
        assertTrue(AnimationPresetCatalog.all().contains(AnimationPresetCatalog.TYPEWRITER));
        assertTrue(AnimationPresetCatalog.all().contains(AnimationPresetCatalog.CHOICE_HOVER));
    }
}
