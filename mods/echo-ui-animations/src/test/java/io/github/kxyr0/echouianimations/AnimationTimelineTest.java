package io.github.kxyr0.echouianimations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnimationTimelineTest {
    @Test
    void fadeInMovesAlphaFromZeroToOne() {
        AnimationFrame start = AnimationTimeline.sample(AnimationPresetCatalog.FADE_IN, 0, 10);
        AnimationFrame end = AnimationTimeline.sample(AnimationPresetCatalog.FADE_IN, 500, 10);

        assertEquals(0.0F, start.alpha());
        assertEquals(1.0F, end.alpha());
        assertTrue(end.finished());
    }

    @Test
    void typewriterRevealsCharactersProgressively() {
        AnimationFrame middle = AnimationTimeline.sample(AnimationPresetCatalog.TYPEWRITER, 180, 20);
        AnimationFrame end = AnimationTimeline.sample(AnimationPresetCatalog.TYPEWRITER, 500, 20);

        assertEquals(10, middle.visibleCharacters());
        assertEquals(20, end.visibleCharacters());
    }

    @Test
    void nonTypewriterPresetsKeepAllCharactersVisible() {
        AnimationFrame frame = AnimationTimeline.sample(AnimationPresetCatalog.PANEL_OPEN, 0, 12);

        assertEquals(12, frame.visibleCharacters());
    }
}
