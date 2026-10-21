package io.github.kxyr0.echoplayerhud.core.layout;

import io.github.kxyr0.echoplayerhud.api.gradient.HudGradient;
import io.github.kxyr0.echoplayerhud.api.layout.HudAnchor;
import io.github.kxyr0.echoplayerhud.api.layout.HudBarDefinition;
import io.github.kxyr0.echoplayerhud.api.layout.HudBarId;
import io.github.kxyr0.echoplayerhud.api.layout.HudBounds;
import io.github.kxyr0.echoplayerhud.api.layout.HudFillDirection;
import io.github.kxyr0.echoplayerhud.api.layout.HudFramebuffer;
import io.github.kxyr0.echoplayerhud.api.layout.HudOrientation;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudReferenceCanvasTransformTest {
    @Test
    void guiScaleDoesNotChangeFramebufferBounds() {
        var bar = new HudDefaultLayout().snapshot().bars().get(0);
        HudReferenceCanvasTransform transform = new HudReferenceCanvasTransform();

        HudBounds scaleOne = transform.resolve(bar, new HudFramebuffer(1920, 1080, 1.0D));
        HudBounds scaleFour = transform.resolve(bar, new HudFramebuffer(1920, 1080, 4.0D));

        assertEquals(scaleOne, scaleFour);
    }

    @Test
    void referenceCanvasScalesToResolution() {
        var bar = new HudDefaultLayout().snapshot().bars().get(0);
        HudBounds bounds = new HudReferenceCanvasTransform().resolve(bar, new HudFramebuffer(3840, 2160, 2.0D));

        assertEquals(72, bounds.x());
        assertEquals(1872, bounds.y());
        assertEquals(520, bounds.width());
        assertEquals(36, bounds.height());
    }

    @Test
    void bottomCenterOffsetsStayRelativeToScreenCenter() {
        HudBarDefinition bar = new HudBarDefinition(new HudBarId("test:centered"), HudMetricIds.HEALTH,
                HudAnchor.BOTTOM_CENTER, HudOrientation.HORIZONTAL, HudFillDirection.LEFT_TO_RIGHT,
                -160, 115, 260, 25, HudGradient.solid(0xFFFFFFFF));

        HudReferenceCanvasTransform transform = new HudReferenceCanvasTransform();
        HudBounds base = transform.resolve(bar, new HudFramebuffer(1920, 1080, 1.0D));
        HudBounds wide = transform.resolve(bar, new HudFramebuffer(2560, 1080, 1.0D));

        assertEquals(670, base.x());
        assertEquals(940, base.y());
        assertEquals(990, wide.x());
        assertEquals(940, wide.y());
    }

    @Test
    void unscaledLayoutKeepsVanillaGuiUnitSize() {
        HudBarDefinition bar = new HudBarDefinition(new HudBarId("test:gui_units"), HudMetricIds.HEALTH,
                HudAnchor.BOTTOM_CENTER, HudOrientation.HORIZONTAL, HudFillDirection.LEFT_TO_RIGHT,
                -160, 115, 260, 25, HudGradient.solid(0xFFFFFFFF));

        HudBounds bounds = new HudReferenceCanvasTransform().resolve(bar,
                new HudFramebuffer(960, 540, 1.0D), 1920, 1080, false);

        assertEquals(190, bounds.x());
        assertEquals(400, bounds.y());
        assertEquals(260, bounds.width());
        assertEquals(25, bounds.height());
    }
}
