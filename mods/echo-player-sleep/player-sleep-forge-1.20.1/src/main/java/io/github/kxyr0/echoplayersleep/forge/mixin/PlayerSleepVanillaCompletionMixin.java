package io.github.kxyr0.echoplayersleep.forge.mixin;

import io.github.kxyr0.echoplayersleep.forge.interception.VanillaSleepCompletionLock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerSleepVanillaCompletionMixin {
    @Inject(method = {"isSleepingLongEnough()Z", "m_36317_()Z"}, at = @At("HEAD"),
            cancellable = true, remap = false)
    private void echoPlayerSleep$holdNarrativeSleeperBeforeDream(CallbackInfoReturnable<Boolean> callback) {
        Player player = (Player) (Object) this;
        if (player instanceof ServerPlayer serverPlayer
                && VanillaSleepCompletionLock.preventsVanillaCompletion(serverPlayer)) {
            callback.setReturnValue(false);
        }
    }
}
