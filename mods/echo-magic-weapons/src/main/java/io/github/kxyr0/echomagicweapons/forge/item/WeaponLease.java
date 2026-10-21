package io.github.kxyr0.echomagicweapons.forge.item;

import io.github.kxyr0.echomagic.forge.api.summoned.SummonedWeaponData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class WeaponLease {
    private WeaponLease() { }

    public static void summon(ServerPlayer player, ArsenalWeaponKind kind, int durationTicks) {
        int existing = find(player);
        if (existing >= 0) restore(player);
        ItemStack original = player.getMainHandItem().copy();
        ItemStack weapon = new ItemStack(ArsenalItems.item(kind));
        weapon.getOrCreateTag().putBoolean("Unbreakable", true);
        weapon.getOrCreateTag().putInt("HideFlags", 4);
        long expires = player.serverLevel().getGameTime() + durationTicks;
        SummonedWeaponData.configure(weapon, player.getUUID(), expires, original, kind.color(), kind.length(),
                kind.motion(), kind.form());
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
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

    public static boolean active(ServerPlayer player, ArsenalWeaponKind kind) {
        ItemStack stack = player.getMainHandItem();
        return ArsenalItems.kind(stack) == kind && SummonedWeaponData.ownedBy(stack, player.getUUID())
                && player.serverLevel().getGameTime() < SummonedWeaponData.expiresAt(stack);
    }

    public static boolean owned(ItemStack stack, ServerPlayer player) {
        return ArsenalItems.kind(stack) != null && SummonedWeaponData.ownedBy(stack, player.getUUID());
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
