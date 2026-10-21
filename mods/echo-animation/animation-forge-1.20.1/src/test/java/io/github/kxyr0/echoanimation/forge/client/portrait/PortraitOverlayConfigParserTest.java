package io.github.kxyr0.echoanimation.forge.client.portrait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.id.FaceControlId;
import io.github.kxyr0.echoanimation.api.math.Vec2;
import io.github.kxyr0.echoanimation.definition.resource.FaceControlDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PortraitOverlayConfigParserTest {
    private final PortraitOverlayConfigParser parser = new PortraitOverlayConfigParser();

    @Test
    void parsesPortraitOverlayConfigCall() {
        var parsed = parser.parse("""
                namespace JDH.Configs.EchoAnimation;
                class JohnFatherPortraitOverlay : PortraitOverlayConfig
                {
                    public override PortraitOverlayDef Build()
                    {
                        return PortraitOverlay("jdh:john_father",
                            centerX: "0.45",
                            centerY: "0.40",
                            offsetX: -2,
                            offsetY: 3,
                            facePixels: "16"
                        );
                    }
                }
                """);

        assertTrue(parsed.isPresent());
        assertEquals("jdh:john_father", parsed.orElseThrow().visualId());
        assertEquals(0.45D, parsed.orElseThrow().config().centerX());
        assertEquals(0.40D, parsed.orElseThrow().config().centerY());
        assertEquals(-2.0D, parsed.orElseThrow().config().offsetX());
        assertEquals(3.0D, parsed.orElseThrow().config().offsetY());
        assertEquals(16.0D, parsed.orElseThrow().config().facePixels());
    }

    @Test
    void parsesPortraitLayerOverrides() {
        var parsed = parser.parse("""
                return PortraitOverlay("jdh:john_father",
                    centerX: "0.50",
                    layers: [
                        Layer("pupil_left", placeX: "-0.07", placeY: "0.01",
                            scaleX: "0.8", scaleY: "0.7", w: 2, h: 3),
                        Layer("glasses", visible: false)
                    ]
                );
                """).orElseThrow();

        PortraitLayerConfig pupil = parsed.config().layer("pupil_left");
        assertEquals(-0.07D, pupil.placeX(0.0D));
        assertEquals(0.01D, pupil.placeY(0.0D));
        assertEquals(0.8D, pupil.resolvedScaleX());
        assertEquals(0.7D, pupil.resolvedScaleY());
        assertEquals(2, pupil.width(1));
        assertEquals(3, pupil.height(1));
        assertTrue(!parsed.config().layer("glasses").isVisible());
    }

    @Test
    void parsesPortraitBlinkOverrides() {
        var parsed = parser.parse("""
                return PortraitOverlay("jdh:john_father",
                    layers: [],
                    idle: [
                        Blink "eye_closure"
                        {
                            Travel(28, -40);
                            Gaze(1, 1);
                            Idle(true);
                            PeriodTicks(30);
                            CloseTicks(2);
                            HoldTicks(2);
                            OpenTicks(2);
                        }
                    ]
                );
                """).orElseThrow();

        FaceControlDefinition base = new FaceControlDefinition(
                FaceControlId.parse("eye_closure"),
                0.035D,
                -0.035D,
                new Vec2(-0.001D, -0.001D),
                new Vec2(0.001D, 0.001D),
                BoneId.parse("upper_eyelids"),
                BoneId.parse("lower_eyelids"),
                false,
                80,
                3,
                2,
                4);

        FaceControlDefinition control = parsed.config().controls(List.of(base)).get(0);
        assertEquals(0.028D, control.blinkUpperTravel(), 0.0001D);
        assertEquals(-0.040D, control.blinkLowerTravel(), 0.0001D);
        assertTrue(control.idleBlink());
        assertEquals(30, control.blinkPeriodTicks());
        assertEquals(2, control.blinkCloseTicks());
        assertEquals(2, control.blinkHoldTicks());
        assertEquals(2, control.blinkOpenTicks());
    }

    @Test
    void missingCallReturnsEmpty() {
        assertTrue(parser.parse("class Empty {}").isEmpty());
    }
}
