package io.github.kxyr0.echoplayervitals.forge.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels vanilla hunger depletion, starvation, sprint food gate, and food-based regeneration.
 */
@Mixin(FoodData.class)
abstract class FoodDataHungerRemovalMixin {
    @Inject(method = {
            "tick(Lnet/minecraft/world/entity/player/Player;)V",
            "m_38710_(Lnet/minecraft/world/entity/player/Player;)V"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private void echoPlayerVitals$disableHunger(Player player, CallbackInfo callback) {
        if (player instanceof ServerPlayer) {
            FoodData food = (FoodData) (Object) this;
            food.setFoodLevel(20);
            food.setSaturation(0.0F);
            food.setExhaustion(0.0F);
            callback.cancel();
        }
    }
}
