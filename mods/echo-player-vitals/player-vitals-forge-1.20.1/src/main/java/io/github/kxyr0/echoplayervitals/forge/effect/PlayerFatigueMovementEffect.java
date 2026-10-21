package io.github.kxyr0.echoplayervitals.forge.effect;

import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueSnapshot;
import io.github.kxyr0.echoplayervitals.api.fatigue.PlayerFatigueStage;
import io.github.kxyr0.echoplayervitals.forge.EchoPlayerVitalsMod;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Applies the source-owned temporary movement modifier for fatigue.
 */
public final class PlayerFatigueMovementEffect {
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes(
            EchoPlayerVitalsMod.MOVEMENT_MODIFIER_ID.getBytes(StandardCharsets.UTF_8));

    public void apply(ServerPlayer player, PlayerFatigueSnapshot fatigue) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        AttributeModifier current = attribute.getModifier(MODIFIER_UUID);
        if (fatigue.stage() == PlayerFatigueStage.RESTED) {
            if (current != null) {
                attribute.removeModifier(MODIFIER_UUID);
            }
            return;
        }
        double amount = fatigue.movementMultiplier() - 1.0D;
        if (current != null && Double.compare(current.getAmount(), amount) == 0) {
            return;
        }
        if (current != null) {
            attribute.removeModifier(MODIFIER_UUID);
        }
        attribute.addTransientModifier(new AttributeModifier(MODIFIER_UUID,
                EchoPlayerVitalsMod.MOVEMENT_MODIFIER_ID, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }
}
