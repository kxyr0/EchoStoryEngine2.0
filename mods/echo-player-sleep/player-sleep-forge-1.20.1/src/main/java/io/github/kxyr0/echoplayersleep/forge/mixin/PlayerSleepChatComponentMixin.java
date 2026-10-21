package io.github.kxyr0.echoplayersleep.forge.mixin;

import io.github.kxyr0.echoplayersleep.forge.client.PlayerSleepChatHider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
abstract class PlayerSleepChatComponentMixin {
    @Inject(method = {"render(Lnet/minecraft/client/gui/GuiGraphics;III)V",
            "m_280165_(Lnet/minecraft/client/gui/GuiGraphics;III)V"}, at = @At("HEAD"),
            cancellable = true, remap = false)
    private void echoPlayerSleep$hideChatWhileSleeping(
            GuiGraphics graphics,
            int tickCount,
            int mouseX,
            int mouseY,
            CallbackInfo callback) {
        if (PlayerSleepChatHider.shouldHideChat()) {
            callback.cancel();
        }
    }
}
