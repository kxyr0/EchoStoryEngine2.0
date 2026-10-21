package io.github.kxyr0.echosparkmagic.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class SparkMagicPlayerState {
    private static final String MOVEMENT_ENABLED =
            "echo_spark_magic.movement_enabled";

    private SparkMagicPlayerState() { }

    public static boolean enabled(Player player) {
        return player.getPersistentData().getBoolean(MOVEMENT_ENABLED);
    }

    public static void setEnabled(Player player, boolean enabled) {
        player.getPersistentData().putBoolean(MOVEMENT_ENABLED, enabled);
    }

    public static void copyAfterClone(PlayerEvent.Clone event) {
        setEnabled(event.getEntity(), enabled(event.getOriginal()));
    }
}

