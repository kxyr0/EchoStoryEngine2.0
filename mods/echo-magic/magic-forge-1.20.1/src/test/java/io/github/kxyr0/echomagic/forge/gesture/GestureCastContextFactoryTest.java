package io.github.kxyr0.echomagic.forge.gesture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.forge.cast.GestureCastContext;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class GestureCastContextFactoryTest {
    @Test void autoAimChoosesNearestTargetInFrontAndIgnoresCloserTargetBehind() {
        Vec3 expected = new Vec3(0.4D, 0.2D, 2.0D);
        var result = GestureCastContextFactory.nearestAim(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D),
                List.of(new Vec3(0.0D, 0.0D, -0.8D), new Vec3(1.2D, 0.4D, 4.0D),
                        expected, new Vec3(8.0D, 0.0D, 0.2D)), 12.0D).orElseThrow();

        assertTrue(result.distanceTo(expected.normalize()) < 1.0E-12D);
    }

    @Test void autoAimFallsBackWhenNoTargetIsInsideRangeAndAimedContextNormalizesDirection() {
        assertTrue(GestureCastContextFactory.nearestAim(Vec3.ZERO, new Vec3(0.0D, 0.0D, 1.0D),
                List.of(new Vec3(0.0D, 0.0D, 13.0D)), 12.0D).isEmpty());
        GestureCastContext context = GestureCastContext.aimed(new Vec3(0.0D, 0.0D, 3.0D));
        assertEquals(new Vec3(0.0D, 0.0D, 1.0D), context.direction().orElseThrow());
        assertEquals(0L, context.initialDelayTicks());
    }
}
