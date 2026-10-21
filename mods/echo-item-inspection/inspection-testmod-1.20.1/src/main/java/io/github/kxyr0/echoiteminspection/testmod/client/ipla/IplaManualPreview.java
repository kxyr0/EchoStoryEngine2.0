package io.github.kxyr0.echoiteminspection.testmod.client.ipla;

import io.github.kxyr0.echoiteminspection.forge.InspectionRuntime;
import io.github.kxyr0.echoiteminspection.testmod.EchoItemInspectionTestMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Separate interactive IPLA scene. No automatic input or shutdown after it becomes ready. */
@Mod.EventBusSubscriber(modid = EchoItemInspectionTestMod.MOD_ID, value = Dist.CLIENT)
public final class IplaManualPreview {
    private static final boolean ENABLED = Boolean.getBoolean("echo_item_inspection.iplaManual");
    private static final IplaSmokeFixture FIXTURE = ENABLED ? new IplaSmokeFixture() : null;
    private static int phase;
    private static int settleTicks;
    private static boolean captured;

    private IplaManualPreview() { }

    @SubscribeEvent public static void capture(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        if (!ENABLED || phase != 3 || captured) return;
        var prompt = io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime.prompt();
        if (prompt.activeTarget() == null || prompt.alpha(net.minecraft.Util.getMillis()) < 0.99) return;
        captured = true;
        var minecraft = Minecraft.getInstance();
        net.minecraft.client.Screenshot.grab(minecraft.gameDirectory, "ipla-manual-ru.png", minecraft.getMainRenderTarget(),
                message -> com.mojang.logging.LogUtils.getLogger().info("IPLA_MANUAL_SCREENSHOT {}", message.getString()));
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || phase == 3 || event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) return;
        try {
            if (phase == 0) {
                minecraft.options.pauseOnLostFocus = false;
                minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                minecraft.setScreen(null);
                description(minecraft.gameDirectory.toPath());
                FIXTURE.prepare(minecraft);
                phase = 1;
            } else if (phase == 1 && FIXTURE.ready(minecraft)) {
                InspectionRuntime.reload();
                addInventory(minecraft);
                phase = 2;
            } else if (phase == 2) {
                FIXTURE.stabilize(minecraft);
                if (++settleTicks < 12 || !minecraft.gameMode.hasInfiniteItems()) return;
                phase = 3;
                com.mojang.logging.LogUtils.getLogger().info("ITEM_INSPECTION_IPLA_MANUAL_READY mode=creative language={}",
                        minecraft.options.languageCode);
            }
        } catch (Exception exception) {
            phase = 3;
            com.mojang.logging.LogUtils.getLogger().error("ITEM_INSPECTION_IPLA_MANUAL_FAILED", exception);
        }
    }

    private static void addInventory(Minecraft minecraft) {
        var id = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(id);
            minecraft.getSingleplayerServer().setDefaultGameType(net.minecraft.world.level.GameType.CREATIVE);
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(-4, 98, -5, 7, 104, 4)).forEach(net.minecraft.world.entity.Entity::discard);
            player.getInventory().setItem(1, IplaSmokeFixture.token());
            player.getInventory().setItem(2, new ItemStack(Items.COBBLESTONE, 64));
            player.getInventory().setItem(3, new ItemStack(Items.STONE, 64));
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(Enchantments.UNBREAKING, 1);
            player.getInventory().setItem(4, sword);
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void description(Path directory) throws IOException {
        Path file = directory.resolve("story/item-inspection/echo_item_inspection_testmod/token.inspection.es");
        if (Files.exists(file)) return;
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                class ManualTokenInspection : ItemInspectionResource {
                    Item("echo_item_inspection_testmod:inspection_token");
                    Description("Тестовый предмет с собственным названием и описанием.\\n\\nЗдесь можно разместить историю предмета, заметки или сведения о его происхождении.");
                }
                """, StandardOpenOption.CREATE_NEW);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { Files.deleteIfExists(file); }
            catch (IOException exception) { com.mojang.logging.LogUtils.getLogger().warn("Cannot remove manual preview text", exception); }
        }, "inspection-ipla-manual-cleanup"));
    }
}
