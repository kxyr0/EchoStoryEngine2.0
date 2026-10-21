package io.github.kxyr0.echomagic.core.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicMagicIdsTest {
    @Test void sameInputProducesSameCastAndStableProjectileIds() {
        MagicPlayerRef player = new MagicPlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var command = DeterministicMagicIds.command(player, new SpellId("test:spell"), 42, 7);
        assertEquals(command, DeterministicMagicIds.command(player, new SpellId("test:spell"), 42, 7));
        var cast = DeterministicMagicIds.cast(command);
        assertNotEquals(DeterministicMagicIds.projectile(cast, 0), DeterministicMagicIds.projectile(cast, 1));
    }
}
