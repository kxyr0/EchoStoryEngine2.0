package io.github.kxyr0.echomagic.core.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerSpellStateTest {
    private final MagicPlayerRef player = new MagicPlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private final SpellId spell = new SpellId("test:spell");

    @Test void grantIsIdempotentAndUnknownSelectionFails() {
        KnownSpellRegistry known = new KnownSpellRegistry(4);
        SelectedSpellRegistry selected = new SelectedSpellRegistry();
        assertFalse(selected.select(player, spell, known));
        assertTrue(known.grant(player, spell));
        assertFalse(known.grant(player, spell));
        assertTrue(selected.select(player, spell, known));
        known.remove(player, spell);
        selected.clearIf(player, spell);
        assertTrue(selected.view(player).selected().isEmpty());
    }

    @Test void cooldownIsServerTickBased() {
        SpellCooldownRegistry cooldowns = new SpellCooldownRegistry();
        assertTrue(cooldowns.ready(player, spell, 10));
        cooldowns.begin(player, spell, 10, 16);
        assertFalse(cooldowns.ready(player, spell, 25));
        assertTrue(cooldowns.ready(player, spell, 26));
    }

    @Test void cooldownPersistsAsRemainingDurationAcrossServerRestart() {
        SpellCooldownRegistry firstServer = new SpellCooldownRegistry();
        firstServer.begin(player, spell, 1_000, 16);
        Map<SpellId, Long> saved = firstServer.remainingTicks(player, 1_006);
        assertFalse(saved.isEmpty());

        SpellCooldownRegistry secondServer = new SpellCooldownRegistry();
        secondServer.restoreRemaining(player, saved, 0);
        assertFalse(secondServer.ready(player, spell, 9));
        assertTrue(secondServer.ready(player, spell, 10));
    }
}
