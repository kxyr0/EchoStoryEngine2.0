package io.github.kxyr0.echoplayersleep.forge.mixin;

import io.github.kxyr0.echoplayersleep.forge.interception.VanillaSleepCompletionLock;
import io.github.kxyr0.echoplayersleep.forge.interception.StorySleepWakeDiagnostics;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
abstract class PlayerSleepServerPlayerWakeMixin {
    @Inject(method = {"stopSleepInBed(ZZ)V", "m_6145_(ZZ)V"}, at = @At("HEAD"),
            cancellable = true, remap = false)
    private void echoPlayerSleep$blockVanillaWakeDuringNarrativeSleep(
            boolean wakeImmediately,
            boolean updateLevel,
            CallbackInfo callback) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        var blocked = VanillaSleepCompletionLock.blockedWakeSession(player);
        if (blocked.isPresent()) {
            StorySleepWakeDiagnostics.logBlockedServerPlayerStop(player, blocked.orElseThrow(),
                    wakeImmediately, updateLevel);
            callback.cancel();
        }
    }
}
