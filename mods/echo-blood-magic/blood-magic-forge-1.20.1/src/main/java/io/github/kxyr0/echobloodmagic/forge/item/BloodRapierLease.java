package io.github.kxyr0.echobloodmagic.forge.item;

import io.github.kxyr0.echomagic.forge.api.summoned.SummonedWeaponData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class BloodRapierLease {
    private static final int TRAIL_COLOR = 0xFF3030;
    private static final float BLADE_LENGTH = 1.35F;
    private BloodRapierLease() { }

    public static void summon(ServerPlayer player, int durationTicks) {
        long expiresAt = player.serverLevel().getGameTime() + durationTicks;
        int existing = find(player);
        if (existing >= 0) {
            equip(player, existing);
            SummonedWeaponData.renew(player.getMainHandItem(), expiresAt);
            player.getInventory().setChanged();
            return;
        }
        ItemStack original = player.getMainHandItem().copy();
        ItemStack rapier = new ItemStack(BloodMagicItems.BLOOD_RAPIER.get());
        rapier.getOrCreateTag().putBoolean("Unbreakable", true);
        rapier.getOrCreateTag().putInt("HideFlags", 4);
        SummonedWeaponData.configure(rapier, player.getUUID(), expiresAt, original,
                TRAIL_COLOR, BLADE_LENGTH, "thrust", "rapier");
        player.setItemInHand(InteractionHand.MAIN_HAND, rapier);
        player.getInventory().setChanged();
    }

    public static State maintain(ServerPlayer player) {
        int slot = find(player);
        if (slot < 0) return State.NONE;
        equip(player, slot);
        if (player.serverLevel().getGameTime() < SummonedWeaponData.expiresAt(player.getMainHandItem())) {
            return State.ACTIVE;
        }
        restore(player);
        return State.EXPIRED;
    }

    public static boolean active(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return owned(stack, player) && player.serverLevel().getGameTime() < SummonedWeaponData.expiresAt(stack);
    }

    public static boolean restore(ServerPlayer player) {
        int slot = find(player);
        if (slot < 0) return false;
        equip(player, slot);
        ItemStack original = SummonedWeaponData.original(player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, original);
        removeDuplicates(player);
        player.getInventory().setChanged();
        return true;
    }

    public static boolean owned(ItemStack stack, ServerPlayer player) {
        return stack.is(BloodMagicItems.BLOOD_RAPIER.get())
                && SummonedWeaponData.ownedBy(stack, player.getUUID());
    }

    private static int find(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (owned(inventory.getItem(slot), player)) return slot;
        }
        return -1;
    }

    private static void equip(ServerPlayer player, int slot) {
        Inventory inventory = player.getInventory();
        int selected = inventory.selected;
        if (slot == selected) return;
        ItemStack displaced = inventory.getItem(selected);
        inventory.setItem(selected, inventory.getItem(slot));
        inventory.setItem(slot, displaced);
    }

    private static void removeDuplicates(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (owned(inventory.getItem(slot), player)) inventory.setItem(slot, ItemStack.EMPTY);
        }
    }

    public enum State { NONE, ACTIVE, EXPIRED }
}
