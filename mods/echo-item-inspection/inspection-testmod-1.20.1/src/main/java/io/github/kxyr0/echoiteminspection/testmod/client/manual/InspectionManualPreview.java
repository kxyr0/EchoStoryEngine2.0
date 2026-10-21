package io.github.kxyr0.echoiteminspection.testmod.client.manual;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoiteminspection.client.api.ItemInspectionClient;
import io.github.kxyr0.echoiteminspection.forge.InspectionRuntime;
import io.github.kxyr0.echoiteminspection.testmod.EchoItemInspectionTestMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in interactive preview; it stays open until the user closes this separate dev client. */
@Mod.EventBusSubscriber(modid = EchoItemInspectionTestMod.MOD_ID, value = Dist.CLIENT)
public final class InspectionManualPreview {
    private static final boolean ENABLED = Boolean.getBoolean("echo_item_inspection.manual");
    private static volatile boolean inventoryPrepared;
    private static int phase;

    private InspectionManualPreview() { }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || phase == 2 || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) return;
        try {
            if (phase == 0) {
                minecraft.options.pauseOnLostFocus = false;
                createDefinition(minecraft.gameDirectory.toPath());
                prepareInventory(minecraft);
                phase = 1;
            } else if (inventoryReady(minecraft)) {
                InspectionRuntime.reload();
                minecraft.setScreen(new InventoryScreen(minecraft.player));
                if (!ItemInspectionClient.openInspection(minecraft.player.getInventory().getItem(0))) {
                    throw new IllegalStateException("Manual clock inspection could not open");
                }
                phase = 2;
                LogUtils.getLogger().info("ITEM_INSPECTION_MANUAL_READY — interactive client stays open");
            }
        } catch (Exception exception) {
            phase = 2;
            LogUtils.getLogger().error("ITEM_INSPECTION_MANUAL_FAILED", exception);
        }
    }

    private static boolean inventoryReady(Minecraft minecraft) {
        var custom = minecraft.player.getInventory().getItem(5);
        return inventoryPrepared && !minecraft.gameMode.hasInfiniteItems()
                && minecraft.player.containerMenu == minecraft.player.inventoryMenu
                && minecraft.player.getInventory().getItem(0).is(Items.CLOCK)
                && custom.hasTag() && custom.getTag().getInt("CustomModelData") == 19;
    }

    private static void prepareInventory(Minecraft minecraft) {
        var id = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(id);
            player.closeContainer();
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);
            player.teleportTo(0.5, 100, 0.5);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
            var inventory = player.getInventory();
            inventory.clearContent();
            inventory.setItem(0, named(new ItemStack(Items.CLOCK), "Часы для осмотра"));
            var token = named(new ItemStack(EchoItemInspectionTestMod.TOKEN.get(), 7), "Предмет с данными");
            token.getOrCreateTag().putString("manualPreview", "exact snapshot");
            inventory.setItem(1, token);
            inventory.setItem(2, new ItemStack(Items.STONE, 64));
            inventory.setItem(3, new ItemStack(Items.COBBLESTONE, 64));
            var sword = named(new ItemStack(Items.DIAMOND_SWORD), "Зачарованный меч");
            sword.enchant(Enchantments.UNBREAKING, 1);
            inventory.setItem(4, sword);
            var custom = named(new ItemStack(EchoItemInspectionTestMod.TOKEN.get()), "Изменённая модель");
            custom.getOrCreateTag().putInt("CustomModelData", 19);
            inventory.setItem(5, custom);
            player.inventoryMenu.broadcastChanges();
            inventoryPrepared = true;
        });
    }

    private static ItemStack named(ItemStack stack, String title) {
        stack.setHoverName(Component.literal(title));
        return stack;
    }

    private static void createDefinition(Path gameDirectory) throws IOException {
        if (InspectionRuntime.catalog().resolve("minecraft:clock").isPresent()) return;
        Path directory = gameDirectory.resolve("story/item-inspection/echo_item_inspection_manual");
        Files.createDirectories(directory);
        Path file = directory.resolve("preview_" + UUID.randomUUID().toString().replace("-", "") + ".inspection.es");
        Files.writeString(file, """
                class ManualInspection : ItemInspectionResource {
                    Item("minecraft:clock");
                    Title("Тест осмотра предмета");
                    Description("Тестовый предмет с собственным названием и описанием.\\n\\nЗдесь можно разместить историю предмета, заметки или сведения о его происхождении.");
                }
                """, StandardOpenOption.CREATE_NEW);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { Files.deleteIfExists(file); }
            catch (IOException exception) { LogUtils.getLogger().warn("Cannot remove manual preview fixture {}", file, exception); }
        }, "inspection-manual-fixture-cleanup"));
    }
}
