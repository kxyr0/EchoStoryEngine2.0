package io.github.kxyr0.echomagic.core.identity;

import io.github.kxyr0.echomagic.api.identity.SpellCastCommandId;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.identity.SpellProjectileId;
import io.github.kxyr0.echomagic.api.player.MagicPlayerRef;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class DeterministicMagicIds {
    private DeterministicMagicIds() { }

    public static SpellCastCommandId command(MagicPlayerRef player, SpellId spell, long tick, long sequence) {
        return new SpellCastCommandId(hash("command", player.toString(), spell.value(), tick, sequence));
    }

    public static SpellCastId cast(SpellCastCommandId command) {
        return new SpellCastId(hash("cast", command.value()));
    }

    public static SpellProjectileId projectile(SpellCastId cast, int emissionIndex) {
        return new SpellProjectileId(hash("projectile", cast.value(), emissionIndex));
    }

    private static String hash(Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object value : values) digest.update((value + "\u0000").getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest.digest()).substring(0, 32);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by Java.", exception);
        }
    }
}

