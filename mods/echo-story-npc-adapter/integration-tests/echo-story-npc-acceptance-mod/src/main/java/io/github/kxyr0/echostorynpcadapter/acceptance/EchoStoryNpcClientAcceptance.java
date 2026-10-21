package io.github.kxyr0.echostorynpcadapter.acceptance;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = EchoStoryNpcAcceptanceMod.MOD_ID, value = Dist.CLIENT)
public final class EchoStoryNpcClientAcceptance {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation ECHO_NPC_ENTITY_TYPE = new ResourceLocation("echo_npc", "echo_npc");
    private static final String ACTOR_ID = "echo_story_npc_test:john_father";
    private static final String WORLD_A = System.getProperty("echo_story_npc_acceptance.worldA", "N3ClientWorldA");
    private static final String WORLD_B = System.getProperty("echo_story_npc_acceptance.worldB", "N3ClientWorldB");
    private static final int TIMEOUT_TICKS = 1600;
    private static final int SNAPSHOT_INTERVAL_TICKS = 10;

    private static Stage stage = Stage.DISABLED;
    private static int stageTicks;
    private static int screenshotTicks;
    private static volatile boolean serverTaskPending;
    private static boolean started;
    private static boolean titleClearRequested;
    private static volatile ActorSnapshot snapshot = ActorSnapshot.unknown();
    private static volatile String asyncFailure = "";
    private static volatile boolean screenshotDone;

    private EchoStoryNpcClientAcceptance() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!started) {
            started = true;
            advance(Stage.WAIT_WORLD_A_INITIAL);
            LOGGER.info("ESNPC-CLIENT-ACCEPTANCE START worldA={} worldB={}", WORLD_A, WORLD_B);
        }
        if (!asyncFailure.isBlank()) {
            fail(client, asyncFailure);
            return;
        }
        stageTicks++;
        if (stageTicks > TIMEOUT_TICKS) {
            fail(client, "timeout stage=" + stage);
            return;
        }
        tick(client);
    }

    private static void tick(Minecraft client) {
        switch (stage) {
            case WAIT_WORLD_A_INITIAL -> {
                if (inWorld(client)) {
                    positionPlayer(client, 0.0D, 65.0D, 4.0D, 0.0D, 65.0D, 0.0D);
                    advance(Stage.WAIT_WORLD_A_POSITIONED);
                }
            }
            case WAIT_WORLD_A_POSITIONED -> {
                if (inWorld(client) && stageTicks > 80 && !serverTaskPending) {
                    startStory(client, "spawn_father", "client_spawn_visible", Stage.WAIT_SPAWN_VISIBLE);
                }
            }
            case WAIT_SPAWN_VISIBLE -> {
                refreshSnapshot(client);
                if (visibleAt(client, 0.0D, 64.0D, 0.0D)) {
                    positionPlayer(client, 0.0D, 65.0D, 4.0D, 0.0D, 65.0D, 0.0D);
                    advance(Stage.WAIT_SPAWN_CAMERA);
                }
            }
            case WAIT_SPAWN_CAMERA -> waitCameraThen(client, "n3-client-spawn-visible.png",
                    Stage.WAIT_SPAWN_SCREENSHOT);
            case WAIT_SPAWN_SCREENSHOT -> waitScreenshotThen(client, Stage.RETURN_TITLE_AFTER_SPAWN);
            case RETURN_TITLE_AFTER_SPAWN -> returnToTitle(client, Stage.WAIT_TITLE_AFTER_SPAWN);
            case WAIT_TITLE_AFTER_SPAWN -> {
                completeReturnToTitle(client);
                if (atTitle(client)) {
                    LOGGER.info("ESNPC-CLIENT-ACCEPTANCE TITLE_CLEARED after_spawn");
                    loadWorld(client, WORLD_A, Stage.WAIT_WORLD_A_RECOVERY);
                }
            }
            case WAIT_WORLD_A_RECOVERY -> {
                refreshSnapshot(client);
                if (visibleAt(client, 0.0D, 64.0D, 0.0D)) {
                    positionPlayer(client, 0.0D, 65.0D, 4.0D, 0.0D, 65.0D, 0.0D);
                    advance(Stage.WAIT_RECOVERY_CAMERA);
                }
            }
            case WAIT_RECOVERY_CAMERA -> waitCameraThen(client, "n3-client-recovery-visible.png",
                    Stage.WAIT_RECOVERY_SCREENSHOT);
            case WAIT_RECOVERY_SCREENSHOT -> waitScreenshotThen(client, Stage.START_TELEPORT);
            case START_TELEPORT -> startStory(client, "teleport_father", "client_teleport_visible",
                    Stage.WAIT_TELEPORT_VISIBLE);
            case WAIT_TELEPORT_VISIBLE -> {
                refreshSnapshot(client);
                if (visibleAt(client, 5.0D, 64.0D, 0.0D)) {
                    positionPlayer(client, 5.0D, 65.0D, 4.0D, 5.0D, 65.0D, 0.0D);
                    advance(Stage.WAIT_TELEPORT_CAMERA);
                }
            }
            case WAIT_TELEPORT_CAMERA -> waitCameraThen(client, "n3-client-teleport-visible.png",
                    Stage.WAIT_TELEPORT_SCREENSHOT);
            case WAIT_TELEPORT_SCREENSHOT -> waitScreenshotThen(client, Stage.START_DESPAWN);
            case START_DESPAWN -> startStory(client, "despawn_father", "client_despawn_visible",
                    Stage.WAIT_DESPAWN_ABSENT);
            case WAIT_DESPAWN_ABSENT -> {
                refreshSnapshot(client);
                if (snapshot.notFound() && clientNpcCount(client, 0.0D, 64.0D, 0.0D, 128.0D) == 0) {
                    LOGGER.info("ESNPC-CLIENT-ACCEPTANCE DESPAWN_ABSENT actor={} clientEntities=0", ACTOR_ID);
                    returnToTitle(client, Stage.WAIT_TITLE_AFTER_DESPAWN);
                }
            }
            case WAIT_TITLE_AFTER_DESPAWN -> {
                completeReturnToTitle(client);
                if (atTitle(client)) {
                    LOGGER.info("ESNPC-CLIENT-ACCEPTANCE TITLE_CLEARED after_despawn");
                    loadWorld(client, WORLD_B, Stage.WAIT_WORLD_B_ISOLATION);
                }
            }
            case WAIT_WORLD_B_ISOLATION -> {
                refreshSnapshot(client);
                if (inWorld(client) && snapshot.notFound()
                        && clientNpcCount(client, 0.0D, 64.0D, 0.0D, 128.0D) == 0) {
                    LOGGER.info("ESNPC-CLIENT-ACCEPTANCE SECOND_WORLD_ISOLATED actor={} clientEntities=0", ACTOR_ID);
                    returnToTitle(client, Stage.WAIT_TITLE_BEFORE_STOP);
                }
            }
            case WAIT_TITLE_BEFORE_STOP -> {
                completeReturnToTitle(client);
                if (atTitle(client)) {
                    LOGGER.info("ESNPC-CLIENT-ACCEPTANCE TITLE_CLEARED before_stop");
                    pass(client);
                }
            }
            default -> {
            }
        }
    }

    private static boolean enabled() {
        return Boolean.getBoolean("echo_story_npc_acceptance.clientAuto");
    }

    private static boolean inWorld(Minecraft client) {
        return client.level != null && client.player != null && client.getSingleplayerServer() != null;
    }

    private static boolean atTitle(Minecraft client) {
        return client.level == null && client.player == null && client.getSingleplayerServer() == null
                && client.screen instanceof TitleScreen;
    }

    private static void startStory(Minecraft client, String storyLeaf, String executionLeaf, Stage next) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null || serverTaskPending) {
            return;
        }
        serverTaskPending = true;
        advance(next);
        server.execute(() -> {
            try {
                EchoStoryExecutionBridge.lookup(server).starter().orElseThrow().start(
                        StoryId.of("echo_story_npc_test:" + storyLeaf),
                        ExecutionId.of("echo_story_npc_test:" + executionLeaf));
                LOGGER.info("ESNPC-CLIENT-ACCEPTANCE STORY_STARTED story={} execution={}", storyLeaf, executionLeaf);
                snapshot = ActorSnapshot.unknown();
            } catch (RuntimeException exception) {
                asyncFailure = "story start failed " + storyLeaf + ": " + exception.getMessage();
            } finally {
                serverTaskPending = false;
            }
        });
    }

    private static void refreshSnapshot(Minecraft client) {
        if (serverTaskPending || stageTicks % SNAPSHOT_INTERVAL_TICKS != 0) {
            return;
        }
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            return;
        }
        serverTaskPending = true;
        server.execute(() -> {
            try {
                NpcResults.Query query = EchoNpcServiceBridge.lookup(server).service().orElseThrow()
                        .query(new ActorId(ACTOR_ID));
                snapshot = ActorSnapshot.from(query);
            } catch (RuntimeException exception) {
                asyncFailure = "actor query failed: " + exception.getMessage();
            } finally {
                serverTaskPending = false;
            }
        });
    }

    private static boolean visibleAt(Minecraft client, double x, double y, double z) {
        ActorSnapshot current = snapshot;
        int clientCount = clientNpcCount(client, x, y, z, 8.0D);
        if (current.materializedAt(x, y, z) && clientCount == 1) {
            LOGGER.info("ESNPC-CLIENT-ACCEPTANCE VISIBLE actor={} x={} y={} z={} clientEntities=1",
                    ACTOR_ID, x, y, z);
            return true;
        }
        return false;
    }

    private static int clientNpcCount(Minecraft client, double x, double y, double z, double radius) {
        if (client.level == null) {
            return 0;
        }
        int count = 0;
        double maxDistance = radius * radius;
        for (Entity entity : client.level.entitiesForRendering()) {
            ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (ECHO_NPC_ENTITY_TYPE.equals(type) && entity.distanceToSqr(x, y, z) <= maxDistance) {
                count++;
            }
        }
        return count;
    }

    private static void positionPlayer(Minecraft client, double x, double y, double z,
            double lookX, double lookY, double lookZ) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null || serverTaskPending) {
            return;
        }
        serverTaskPending = true;
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                        ? null : server.getPlayerList().getPlayers().get(0);
                if (player != null) {
                    ServerLevel level = server.overworld();
                    player.setGameMode(GameType.CREATIVE);
                    player.teleportTo(level, x, y, z, Set.of(), player.getYRot(), player.getXRot());
                    player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(lookX, lookY, lookZ));
                }
            } finally {
                serverTaskPending = false;
            }
        });
    }

    private static void screenshot(Minecraft client, String fileName, Stage next) {
        screenshotDone = false;
        Screenshot.grab(client.gameDirectory, fileName, client.getMainRenderTarget(), message -> {
            LOGGER.info("ESNPC-CLIENT-ACCEPTANCE SCREENSHOT {} {}", fileName, message.getString());
            screenshotDone = true;
        });
        screenshotTicks = 0;
        advance(next);
    }

    private static void waitCameraThen(Minecraft client, String fileName, Stage next) {
        if (stageTicks > 40) {
            screenshot(client, fileName, next);
        }
    }

    private static void waitScreenshotThen(Minecraft client, Stage next) {
        screenshotTicks++;
        if (screenshotDone || screenshotTicks > 80) {
            advance(next);
        }
    }

    private static void returnToTitle(Minecraft client, Stage next) {
        titleClearRequested = false;
        if (client.getConnection() != null) {
            client.getConnection().getConnection().disconnect(Component.literal("N3 client acceptance"));
        }
        client.setScreen(new TitleScreen());
        snapshot = ActorSnapshot.unknown();
        advance(next);
    }

    private static void completeReturnToTitle(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();
        if (!titleClearRequested && (server == null || !server.isRunning())) {
            titleClearRequested = true;
            client.clearLevel(new TitleScreen());
        }
    }

    private static void loadWorld(Minecraft client, String worldName, Stage next) {
        client.createWorldOpenFlows().loadLevel(new TitleScreen(), worldName);
        snapshot = ActorSnapshot.unknown();
        advance(next);
    }

    private static void pass(Minecraft client) {
        LOGGER.info("ESNPC-CLIENT-ACCEPTANCE PASS visibleSpawn=true recovery=true teleport=true despawn=true "
                + "titleClear=true secondWorldIsolation=true");
        advance(Stage.STOPPING);
        client.stop();
    }

    private static void fail(Minecraft client, String reason) {
        LOGGER.error("ESNPC-CLIENT-ACCEPTANCE FAIL {}", reason);
        advance(Stage.STOPPING);
        client.stop();
    }

    private static void advance(Stage next) {
        stage = next;
        stageTicks = 0;
        LOGGER.info("ESNPC-CLIENT-ACCEPTANCE STAGE {}", next);
    }

    private enum Stage {
        DISABLED,
        WAIT_WORLD_A_INITIAL,
        WAIT_WORLD_A_POSITIONED,
        WAIT_SPAWN_VISIBLE,
        WAIT_SPAWN_CAMERA,
        WAIT_SPAWN_SCREENSHOT,
        RETURN_TITLE_AFTER_SPAWN,
        WAIT_TITLE_AFTER_SPAWN,
        WAIT_WORLD_A_RECOVERY,
        WAIT_RECOVERY_CAMERA,
        WAIT_RECOVERY_SCREENSHOT,
        START_TELEPORT,
        WAIT_TELEPORT_VISIBLE,
        WAIT_TELEPORT_CAMERA,
        WAIT_TELEPORT_SCREENSHOT,
        START_DESPAWN,
        WAIT_DESPAWN_ABSENT,
        WAIT_TITLE_AFTER_DESPAWN,
        WAIT_WORLD_B_ISOLATION,
        WAIT_TITLE_BEFORE_STOP,
        STOPPING
    }

    private record ActorSnapshot(boolean known, boolean found, NpcActorView.Status status, double x, double y,
            double z) {
        static ActorSnapshot unknown() {
            return new ActorSnapshot(false, false, NpcActorView.Status.HOST_STOPPED, 0.0D, 0.0D, 0.0D);
        }

        static ActorSnapshot from(NpcResults.Query query) {
            if (query.status() != NpcResults.QueryStatus.FOUND || query.actor().isEmpty()) {
                return new ActorSnapshot(true, false, NpcActorView.Status.HOST_STOPPED, 0.0D, 0.0D, 0.0D);
            }
            NpcActorView actor = query.actor().orElseThrow();
            return new ActorSnapshot(true, true, actor.status(), actor.position().x(), actor.position().y(),
                    actor.position().z());
        }

        boolean notFound() {
            return known && !found;
        }

        boolean materializedAt(double expectedX, double expectedY, double expectedZ) {
            return found && status == NpcActorView.Status.MATERIALIZED
                    && x == expectedX && y == expectedY && z == expectedZ;
        }
    }
}
