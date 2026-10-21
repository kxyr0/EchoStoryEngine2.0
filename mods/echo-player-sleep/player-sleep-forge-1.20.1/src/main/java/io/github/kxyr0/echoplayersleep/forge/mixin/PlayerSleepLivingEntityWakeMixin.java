package io.github.kxyr0.echoplayersleep.forge.mixin;

import io.github.kxyr0.echoplayersleep.forge.interception.StorySleepWakeDiagnostics;
import io.github.kxyr0.echoplayersleep.forge.interception.VanillaSleepCompletionLock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class PlayerSleepLivingEntityWakeMixin {
    @Inject(method = {"stopSleeping()V", "m_5796_()V"}, at = @At("HEAD"),
            cancellable = true, remap = false)
    private void echoPlayerSleep$blockLivingWakeDuringNarrativeSleep(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        var blocked = VanillaSleepCompletionLock.blockedWakeSession(player);
        if (blocked.isPresent()) {
            StorySleepWakeDiagnostics.logBlockedLivingStop(player, blocked.orElseThrow());
            callback.cancel();
        }
    }
}
