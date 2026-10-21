package io.github.kxyr0.echoeffects.forge.movement;

import io.github.kxyr0.echoeffects.forge.EchoEffectsMod;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ForgeMovementContribution {
    private static final UUID MODIFIER_UUID = UUID.nameUUIDFromBytes(
            "echo_effects:movement_speed".getBytes(StandardCharsets.UTF_8));

    public void apply(ServerPlayer player, double multiplier) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        AttributeModifier current = attribute.getModifier(MODIFIER_UUID);
        if (Math.abs(multiplier - 1.0D) < 0.0001D) {
            remove(attribute, current);
            return;
        }
        double amount = multiplier - 1.0D;
        if (current != null && Double.compare(current.getAmount(), amount) == 0) {
            return;
        }
        remove(attribute, current);
        attribute.addTransientModifier(new AttributeModifier(MODIFIER_UUID,
                EchoEffectsMod.MOD_ID + ":movement_speed", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private void remove(AttributeInstance attribute, AttributeModifier current) {
        if (current != null) {
            attribute.removeModifier(MODIFIER_UUID);
        }
    }
}
