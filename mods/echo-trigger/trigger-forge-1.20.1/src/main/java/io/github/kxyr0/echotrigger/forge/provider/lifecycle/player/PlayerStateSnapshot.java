package io.github.kxyr0.echotrigger.forge.provider.lifecycle.player;

import net.minecraft.server.level.ServerPlayer;

record PlayerStateSnapshot(
        float health,
        int food,
        int experienceLevel,
        boolean sneaking,
        boolean sprinting,
        boolean swimming,
        boolean sleeping,
        boolean onGround,
        boolean burning,
        double yVelocity
) {
    static PlayerStateSnapshot of(ServerPlayer player) {
        return new PlayerStateSnapshot(player.getHealth(), player.getFoodData().getFoodLevel(),
                player.experienceLevel, player.isShiftKeyDown(), player.isSprinting(),
                player.isSwimming(), player.isSleeping(), player.onGround(), player.isOnFire(),
                player.getDeltaMovement().y);
    }
}
