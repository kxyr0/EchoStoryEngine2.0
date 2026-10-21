package io.github.kxyr0.echosparkmagic.forge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class SparkMagicMovementTrailTest {
    @Test
    void ignoresTinyPositionNoise() {
        assertFalse(SparkMagicMovementTrail.hasMovement(Vec3.ZERO,
                new Vec3(0.019D, 0.0D, 0.0D)));
    }

    @Test
    void acceptsHorizontalAndVerticalMovement() {
        assertTrue(SparkMagicMovementTrail.hasMovement(Vec3.ZERO,
                new Vec3(0.021D, 0.0D, 0.0D)));
        assertTrue(SparkMagicMovementTrail.hasMovement(Vec3.ZERO,
                new Vec3(0.0D, 0.03D, 0.0D)));
    }
}

