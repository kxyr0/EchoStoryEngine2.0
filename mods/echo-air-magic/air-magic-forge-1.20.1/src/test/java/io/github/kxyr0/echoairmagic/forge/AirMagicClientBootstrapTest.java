package io.github.kxyr0.echoairmagic.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.forge.api.client.particle.MagicParticleMotion;
import org.junit.jupiter.api.Test;

class AirMagicClientBootstrapTest {
    @Test
    void airParticleDriftsOnlyUpward() {
        MagicParticleMotion motion = AirMagicClientBootstrap.airMotion();

        assertEquals(0.0D, motion.x());
        assertTrue(motion.y() > 0.0D);
        assertEquals(0.0D, motion.z());
    }
}

