package io.github.kxyr0.echoitem.forge.reload;

import io.github.kxyr0.echoitem.forge.item.EchoWeaponItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.item.ItemStack;

/**
 * Reapplies equipped Echo weapon stacks so vanilla attribute caches see hot-reloaded modifiers.
 */
public final class EquippedWeaponRefresher {
    public void refresh(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(this::refreshPlayer);
    }

    public void refreshPlayer(ServerPlayer player) {
        refreshEntity(player);
    }

    public void refreshEntity(LivingEntity entity) {
        refreshSlot(entity, EquipmentSlot.MAINHAND);
        refreshSlot(entity, EquipmentSlot.OFFHAND);
    }

    private void refreshSlot(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.getItem() instanceof EchoWeaponItem) {
            AttributeMap attributes = entity.getAttributes();
            var modifiers = stack.getAttributeModifiers(slot);
            attributes.removeAttributeModifiers(modifiers);
            attributes.addTransientAttributeModifiers(modifiers);
        }
    }
}
