package io.github.kxyr0.echomagic.forge.api.client.particle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MagicParticleMotionTest {
    @Test
    void supportsStyleSpecificUpwardMotion() {
        MagicParticleMotion motion = new MagicParticleMotion(0.0D, 0.04D, 0.0D);
        assertEquals(0.04D, motion.y());
        assertEquals(MagicParticleMotion.NONE, new MagicParticleMotion(0.0D, 0.0D, 0.0D));
    }

    @Test
    void rejectsNonFiniteMotion() {
        assertThrows(IllegalArgumentException.class,
                () -> new MagicParticleMotion(0.0D, Double.NaN, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new MagicParticleMotion(Double.POSITIVE_INFINITY, 0.0D, 0.0D));
    }
}
