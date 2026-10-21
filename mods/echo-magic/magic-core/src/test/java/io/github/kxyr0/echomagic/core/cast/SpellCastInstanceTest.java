package io.github.kxyr0.echomagic.core.cast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import io.github.kxyr0.echomagic.core.SpellTestFixture;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpellCastInstanceTest {
    @Test void burstUsesPinnedRevisionAndTwoTickIntervals() {
        SpellCastInstance cast = new SpellCastInstance(new SpellCastId("cast"),
                new MagicPlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                SpellTestFixture.definition("pinned", 1.6), 100);
        assertTrue(cast.emissionDue(100));
        cast.emitted(new SpellProjectileId("p0"));
        assertEquals(102, cast.nextEmissionTick());
        assertFalse(cast.emissionDue(101));
        assertEquals("pinned", cast.definition().revision().value());
    }

    @Test void supportsDelayedAndExternallyTriggeredFirstEmission() {
        MagicPlayerRef player = new MagicPlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        SpellCastInstance delayed = new SpellCastInstance(new SpellCastId("delayed"), player,
                SpellTestFixture.definition("delayed", 1.6), 10L, 30L);
        assertFalse(delayed.emissionDue(29L));
        assertTrue(delayed.emissionDue(30L));
        SpellCastInstance seal = new SpellCastInstance(new SpellCastId("seal"), player,
                SpellTestFixture.definition("seal", 1.6), 10L, Long.MAX_VALUE);
        assertFalse(seal.emissionDue(100L));
        seal.activate(100L);
        assertTrue(seal.emissionDue(100L));
    }
}
