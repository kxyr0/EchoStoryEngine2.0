package io.github.kxyr0.echoteleportmagic.testmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "echo_teleport_magic_test", value = Dist.CLIENT)
public final class TeleportPreviewKey {
    private static final KeyMapping REPEAT = new KeyMapping("Повторить телепортацию (тест)",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "Echo Teleport Test");
    private static boolean announced;
    private static long closeAfter = Long.MAX_VALUE;
    private TeleportPreviewKey() { }
    @Mod.EventBusSubscriber(modid = "echo_teleport_magic_test", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(REPEAT); }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) { announced = false; return; }
        if (Boolean.getBoolean("etm.manual") && TeleportAutoSmoke.subjectId() == null) return;
        if (!announced) {
            announced = true;
            mc.player.displayClientMessage(Component.literal("R — ЗАПУСТИТЬ ЭФФЕКТ. До нажатия тест ждёт. После полного прогона клиент закроется."), false);
        }
        while (REPEAT.consumeClick()) {
            if (io.github.kxyr0.echoteleportmagic.forge.client.TeleportClient.hasVisuals()) continue;
            io.github.kxyr0.echoteleportmagic.forge.client.TeleportClient.clear();
            String target = TeleportAutoSmoke.subjectId() == null ? "@e[type=minecraft:armor_stand,sort=nearest,limit=1]"
                    : TeleportAutoSmoke.subjectId().toString();
            mc.player.connection.sendCommand(Boolean.getBoolean("etm.manual")
                    ? "echo_teleport_magic teleport_xyz " + target + " 8 -60 0"
                    : "echo_teleport_magic preview " + target);
            if (Boolean.getBoolean("etm.manual")) closeAfter = mc.level.getGameTime()
                    + io.github.kxyr0.echoteleportmagic.timeline.TeleportTimeline.COMPLETE_TICK + 30;
        }
        if (Boolean.getBoolean("etm.manual") && mc.level.getGameTime() >= closeAfter) {
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("etm-render-profile-manual.json"), TeleportRenderProbe.report()); }
            catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
            mc.stop();
        }
    }
}
