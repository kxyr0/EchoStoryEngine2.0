package io.github.kxyr0.echoplayersleep.forge.mixin;

import io.github.kxyr0.echoplayersleep.forge.client.PlayerSleepChatHider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class PlayerSleepMinecraftScreenMixin {
    @Inject(method = {"setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            "m_91152_(Lnet/minecraft/client/gui/screens/Screen;)V"}, at = @At("HEAD"),
            cancellable = true, remap = false)
    private void echoPlayerSleep$blockChatScreenWhileSleeping(Screen screen, CallbackInfo callback) {
        if (PlayerSleepChatHider.shouldBlockScreen(screen)) {
            callback.cancel();
        }
    }
}
