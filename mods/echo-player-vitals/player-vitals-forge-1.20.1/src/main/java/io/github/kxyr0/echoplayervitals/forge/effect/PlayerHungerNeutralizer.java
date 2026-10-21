package io.github.kxyr0.echoplayervitals.forge.effect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * Keeps vanilla hunger in a compatibility state without treating it as gameplay resource.
 */
public final class PlayerHungerNeutralizer {
    public void neutralize(ServerPlayer player) {
        FoodData food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(0.0F);
        food.setExhaustion(0.0F);
    }
}
