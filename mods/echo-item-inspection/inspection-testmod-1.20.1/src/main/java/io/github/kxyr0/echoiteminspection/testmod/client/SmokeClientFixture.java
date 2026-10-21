package io.github.kxyr0.echoiteminspection.testmod.client;

import io.github.kxyr0.echoiteminspection.testmod.EchoItemInspectionTestMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

final class SmokeClientFixture {
    private volatile boolean prepared;

    void prepare(Minecraft minecraft) {
        var id = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(id);
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);
            player.teleportTo(0.5, 100.0, 0.5);
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
            player.getInventory().clearContent();
            player.getInventory().setItem(0, token());
            player.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 64));
            player.inventoryMenu.broadcastChanges();
            prepared = true;
        });
    }

    boolean ready(Minecraft minecraft) {
        return prepared && !minecraft.gameMode.hasInfiniteItems()
                && ItemStack.matches(token(), minecraft.player.getInventory().getItem(0))
                && minecraft.player.getInventory().getItem(1).getCount() == 64;
    }

    static ItemStack token() {
        var stack = new ItemStack(EchoItemInspectionTestMod.TOKEN.get(), 7);
        stack.setHoverName(Component.literal("Inspection smoke token"));
        stack.getOrCreateTag().putString("inspectionSmoke", "exact NBT snapshot");
        stack.getOrCreateTag().putInt("inspectionValue", 173);
        return stack;
    }

    static ItemStack customModel() {
        var stack = token();
        stack.getOrCreateTag().putInt("CustomModelData", 19);
        return stack;
    }

    static ItemStack glint() {
        var stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.setDamageValue(17);
        stack.enchant(Enchantments.UNBREAKING, 1);
        stack.getOrCreateTag().putString("inspectionSmoke", "enchanted damage snapshot");
        return stack;
    }
}
