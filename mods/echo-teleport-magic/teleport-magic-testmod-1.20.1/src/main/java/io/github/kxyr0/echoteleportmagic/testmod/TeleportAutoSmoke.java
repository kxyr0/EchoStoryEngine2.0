package io.github.kxyr0.echoteleportmagic.testmod;

import io.github.kxyr0.echoteleportmagic.api.TeleportAnchor;
import io.github.kxyr0.echoteleportmagic.forge.client.TeleportClient;
import io.github.kxyr0.echoteleportmagic.forge.network.TeleportVisualPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Dev-only opt-in fixture, isolated world and deterministic checkpoint captures. */
@Mod.EventBusSubscriber(modid = "echo_teleport_magic_test", value = Dist.CLIENT)
public final class TeleportAutoSmoke {
    private static boolean opened, placed, armed;
    private static int ticks, frame, checkpoint;
    private static volatile UUID subject;
    private static volatile TeleportAnchor subjectAnchor;
    private static boolean finished;
    private static final double[] TIMES = {0, .3, .8, 1.2, 1.55, 1.8, 1.95, 2.1, 2.4, 3, 3.6, 4};
    private static final UUID EXECUTION = UUID.nameUUIDFromBytes("etm-live-reference-173".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private TeleportAutoSmoke() { }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("etm.smoke") || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (!opened && mc.screen instanceof TitleScreen) {
            opened = true;
            mc.createWorldOpenFlows().createFreshLevel("ETM-Smoke-" + System.currentTimeMillis(),
                    new LevelSettings("ETM isolated smoke", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(173, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (++ticks % 20 == 0) {
            try { Files.writeString(Path.of("etm-smoke-state.txt"), "ticks=" + ticks + " screen=" + mc.screen
                    + " placed=" + placed + " subject=" + subject + " frame=" + frame + " checkpoint=" + checkpoint); }
            catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
        }
        if (placed && subject == null && ticks % 20 == 0 && net.minecraftforge.fml.ModList.get().isLoaded("echo_npc")) {
            mc.getSingleplayerServer().execute(() -> NpcSmokeFixture.find(mc.getSingleplayerServer()).ifPresent(npc -> {
                if (Boolean.getBoolean("etm.seated")) NpcSmokeFixture.seat(mc.getSingleplayerServer(), npc);
                subjectAnchor = io.github.kxyr0.echoteleportmagic.forge.server.TeleportSubjects.anchor(npc);
                subject = npc.getUUID();
            }));
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (ticks < 80) return;
        if (!placed) {
            placed = true;
            mc.options.hideGui = !Boolean.getBoolean("etm.manual");
            mc.getSingleplayerServer().execute(() -> place(mc));
        }
        if (subject == null || ticks < 160) return;
        if (Boolean.getBoolean("etm.manual")) return;
        mc.player.setYRot(0); mc.player.setXRot(4); mc.player.yRotO = 0; mc.player.xRotO = 4;
        if (checkpoint >= TIMES.length) return;
        if (!armed) {
            TeleportClient.clear();
            TeleportClient.receive(new TeleportVisualPacket(EXECUTION, subject,
                    subjectAnchor, mc.level.getGameTime(), 173,
                    TeleportVisualPacket.Kind.START, true, false, -1));
            TeleportClient.receive(new TeleportVisualPacket(EXECUTION, subject,
                    subjectAnchor, 0, 173,
                    TeleportVisualPacket.Kind.DEBUG, true, false, TIMES[checkpoint]));
            frame = 0;
            armed = true;
        }
    }
    private static void place(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        var level = server.overworld();
        var player = server.getPlayerList().getPlayers().get(0);
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        player.teleportTo(level, 0, -60, -5, 0, 4);
        if (net.minecraftforge.fml.ModList.get().isLoaded("echo_npc")) { NpcSmokeFixture.spawn(server); return; }
        var entity = new net.minecraft.world.entity.decoration.ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, level);
        entity.moveTo(0, -60, 0, 180, 0); entity.setNoGravity(true);
        entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
        entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD));
        level.addFreshEntity(entity);
        subjectAnchor = io.github.kxyr0.echoteleportmagic.forge.server.TeleportSubjects.anchor(entity);
        subject = entity.getUUID();
    }
    @SubscribeEvent public static void screenshot(RenderLevelStageEvent event) {
        if (!Boolean.getBoolean("etm.smoke") || !armed || subject == null || ticks < 160
                || checkpoint >= TIMES.length || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (++frame < 60) return;
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, String.format(java.util.Locale.ROOT, "etm-%.2f.png", TIMES[checkpoint]),
                mc.getMainRenderTarget(), message -> { });
        try {
            var visual = TeleportClient.subject(subject);
            Files.writeString(Path.of("etm-capture-times.txt"), "checkpoint=" + TIMES[checkpoint]
                    + " actual=" + (visual == null ? "absent" : visual.time(event.getPartialTick())) + "\n",
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
        checkpoint++; frame = 0; armed = false;
        if (checkpoint == TIMES.length) {
            TeleportClient.clear();
            try {
                Files.writeString(Path.of("etm-smoke-complete.txt"), "LIVE_RENDER_CAPTURED: 12 checkpoints, vanilla renderer, armor stand fixture\n");
                Files.writeString(Path.of("etm-render-profile.json"), TeleportRenderProbe.report());
            }
            catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
            mc.options.hideGui = false;
            if (!finished) {
                finished = true;
                mc.getSingleplayerServer().execute(() -> TeleportGameplaySmoke.start(mc.getSingleplayerServer(), subject));
            }
        }
    }
    public static UUID subjectId() { return subject; }
}
