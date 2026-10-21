package io.github.kxyr0.echoplayerhud.core.render;

import io.github.kxyr0.echoplayerhud.api.layout.HudBounds;
import io.github.kxyr0.echoplayerhud.api.layout.HudFillDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudBarFillTest {
    private final HudBarFill fill = new HudBarFill();
    private final HudBounds bounds = new HudBounds(10, 20, 100, 40);

    @Test
    void horizontalDirectionsFillExpectedBounds() {
        assertEquals(new HudBounds(10, 20, 25, 40),
                fill.fill(bounds, HudFillDirection.LEFT_TO_RIGHT, 0.25D));
        assertEquals(new HudBounds(85, 20, 25, 40),
                fill.fill(bounds, HudFillDirection.RIGHT_TO_LEFT, 0.25D));
    }

    @Test
    void verticalDirectionsFillExpectedBounds() {
        assertEquals(new HudBounds(10, 20, 100, 10),
                fill.fill(bounds, HudFillDirection.TOP_TO_BOTTOM, 0.25D));
        assertEquals(new HudBounds(10, 50, 100, 10),
                fill.fill(bounds, HudFillDirection.BOTTOM_TO_TOP, 0.25D));
    }

    @Test
    void clampsFillToBounds() {
        assertEquals(new HudBounds(10, 20, 100, 40),
                fill.fill(bounds, HudFillDirection.LEFT_TO_RIGHT, 2.0D));
        assertEquals(new HudBounds(10, 20, 0, 40),
                fill.fill(bounds, HudFillDirection.LEFT_TO_RIGHT, -1.0D));
    }
}
