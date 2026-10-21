package io.github.kxyr0.echostorynpcadapter.acceptance;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import net.minecraft.core.BlockPos;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@GameTestHolder(EchoStoryNpcAcceptanceMod.MOD_ID)
public final class EchoStoryNpcAcceptanceGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ACTOR_ID = "echo_story_npc_test:john_father";
    private static final String DORMANT_ACTOR_ID = "echo_story_npc_test:dormant_father";
    private static final String TEST_NAME = "EchoStoryNpcAcceptanceGameTests.storySpawnTeleportDespawnSequence";
    private static final String DORMANT_TEST_NAME =
            "EchoStoryNpcAcceptanceGameTests.storySpawnDormantChunkThenMaterializesOnce";
    private static final double DORMANT_X = 4096.0D;
    private static final double DORMANT_Y = 64.0D;
    private static final double DORMANT_Z = 0.0D;
    private static final int DORMANT_CHUNK_X = 256;
    private static final int DORMANT_CHUNK_Z = 0;
    private static final ResourceLocation ECHO_NPC_ENTITY_TYPE = new ResourceLocation("echo_npc", "echo_npc");
    private static final String RUN_SUFFIX = Long.toString(Math.abs(System.nanoTime()), 36);

    private EchoStoryNpcAcceptanceGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 760)
    public static void storySpawnDormantChunkThenMaterializesOnce(GameTestHelper helper) {
        mark(DORMANT_TEST_NAME);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = server.overworld();
        ActorId actorId = new ActorId(DORMANT_ACTOR_ID);
        level.setChunkForced(DORMANT_CHUNK_X, DORMANT_CHUNK_Z, false);
        service(server).despawn(actorId, new ActorMutationId("echo_story_npc_test:cleanup_" + RUN_SUFFIX));
        helper.runAfterDelay(80L, () -> {
            helper.assertFalse(level.hasChunkAt(dormantBlock()), "dormant target chunk must start unloaded");
            helper.assertTrue(service(server).query(actorId).status() == NpcResults.QueryStatus.NOT_FOUND,
                    "dormant actor cleanup must leave actor absent before Story start");
            start(server, "spawn_dormant_father", "spawn_dormant_gt_" + RUN_SUFFIX);
        });
        helper.runAfterDelay(180L, () -> {
            NpcActorView actor = requireActor(helper, server, DORMANT_ACTOR_ID);
            helper.assertTrue(actor.status() == NpcActorView.Status.DORMANT_CHUNK_UNLOADED,
                    "spawn into unloaded chunk must remain dormant, not absent");
            helper.assertTrue(!actor.materializedPosition().isPresent(),
                    "dormant actor must not expose a materialized position");
            helper.assertTrue(entityCount(level, DORMANT_ACTOR_ID) == 0,
                    "unloaded dormant chunk must not contain a physical Echo NPC entity");
            level.setChunkForced(DORMANT_CHUNK_X, DORMANT_CHUNK_Z, true);
        });
        helper.runAfterDelay(300L, () -> {
            NpcActorView actor = requireActor(helper, server, DORMANT_ACTOR_ID);
            helper.assertTrue(materialized(actor), "loaded dormant chunk must materialize the actor");
            helper.assertTrue(actor.position().x() == DORMANT_X, "dormant actor must keep durable x position");
        });
        helper.runAfterDelay(420L, () -> {
            int count = entityCount(level, DORMANT_ACTOR_ID);
            helper.assertTrue(count == 1,
                    "materialized dormant actor must have exactly one physical Echo NPC entity, count=" + count);
            start(server, "spawn_dormant_father", "spawn_dormant_already_gt_" + RUN_SUFFIX);
        });
        helper.runAfterDelay(560L, () -> {
            NpcActorView actor = requireActor(helper, server, DORMANT_ACTOR_ID);
            helper.assertTrue(materialized(actor), "repeated dormant spawn must keep actor materialized");
            int count = entityCount(level, DORMANT_ACTOR_ID);
            helper.assertTrue(count == 1,
                    "repeated dormant spawn must not duplicate the physical Echo NPC entity, count=" + count);
            level.setChunkForced(DORMANT_CHUNK_X, DORMANT_CHUNK_Z, false);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 380)
    public static void storySpawnTeleportDespawnSequence(GameTestHelper helper) {
        mark(TEST_NAME);
        MinecraftServer server = helper.getLevel().getServer();
        server.overworld().getChunk(0, 0);
        start(server, "spawn_father", "spawn_gt_" + RUN_SUFFIX);
        helper.runAfterDelay(70L, () -> {
            NpcActorView actor = requireActor(helper, server);
            helper.assertTrue(materialized(actor), "spawn story must materialize the actor");
            helper.assertTrue(actor.displayName().equals("Отец"), "Russian display name must be preserved");
            helper.assertTrue(actor.position().x() == 0.0D, "spawn story must place actor at x=0");
            start(server, "spawn_father", "spawn_already_gt_" + RUN_SUFFIX);
        });
        helper.runAfterDelay(120L, () -> {
            NpcActorView actor = requireActor(helper, server);
            helper.assertTrue(materialized(actor), "second spawn story must keep actor materialized");
            helper.assertTrue(actor.position().x() == 0.0D, "second spawn story must keep actor at x=0");
            start(server, "teleport_father", "teleport_gt_" + RUN_SUFFIX);
        });
        helper.runAfterDelay(170L, () -> {
            NpcActorView actor = requireActor(helper, server);
            helper.assertTrue(materialized(actor), "teleport story must keep actor materialized");
            helper.assertTrue(actor.position().x() == 5.0D, "teleport story must move actor to x=5");
            start(server, "teleport_father", "teleport_already_gt_" + RUN_SUFFIX);
        });
        helper.runAfterDelay(220L, () -> {
            NpcActorView actor = requireActor(helper, server);
            helper.assertTrue(materialized(actor), "second teleport story must keep actor materialized");
            helper.assertTrue(actor.position().x() == 5.0D, "second teleport story must keep actor at x=5");
            start(server, "despawn_father", "despawn_gt_" + RUN_SUFFIX);
        });
        helper.runAfterDelay(270L, () -> {
            NpcResults.Query query = service(server).query(new ActorId(ACTOR_ID));
            helper.assertTrue(query.status() == NpcResults.QueryStatus.NOT_FOUND,
                    "despawn story must leave actor absent");
            start(server, "despawn_father", "despawn_already_gt_" + RUN_SUFFIX);
        });
        helper.runAfterDelay(330L, () -> {
            NpcResults.Query query = service(server).query(new ActorId(ACTOR_ID));
            helper.assertTrue(query.status() == NpcResults.QueryStatus.NOT_FOUND,
                    "second despawn story must keep actor absent");
            helper.succeed();
        });
    }

    private static void start(MinecraftServer server, String storyLeaf, String executionLeaf) {
        EchoStoryExecutionBridge.lookup(server).starter().orElseThrow().start(
                StoryId.of("echo_story_npc_test:" + storyLeaf),
                ExecutionId.of("echo_story_npc_test:" + executionLeaf));
    }

    private static NpcActorView requireActor(GameTestHelper helper, MinecraftServer server) {
        return requireActor(helper, server, ACTOR_ID);
    }

    private static NpcActorView requireActor(GameTestHelper helper, MinecraftServer server, String rawActorId) {
        NpcResults.Query query = service(server).query(new ActorId(rawActorId));
        helper.assertTrue(query.status() == NpcResults.QueryStatus.FOUND, "actor must be queryable");
        return query.actor().orElseThrow();
    }

    private static NpcService service(MinecraftServer server) {
        return EchoNpcServiceBridge.lookup(server).service().orElseThrow();
    }

    private static boolean materialized(NpcActorView actor) {
        return actor.status() == NpcActorView.Status.MATERIALIZED
                || actor.status() == NpcActorView.Status.ACTIVE
                || materializedPosition(actor).isPresent();
    }

    private static Optional<?> materializedPosition(NpcActorView actor) {
        return actor.materializedPosition();
    }

    private static BlockPos dormantBlock() {
        return BlockPos.containing(DORMANT_X, DORMANT_Y, DORMANT_Z);
    }

    private static int entityCount(ServerLevel level, String actorId) {
        AABB bounds = new AABB(DORMANT_X, DORMANT_Y, DORMANT_Z, DORMANT_X, DORMANT_Y, DORMANT_Z).inflate(8.0D);
        return level.getEntities((Entity) null, bounds, EchoStoryNpcAcceptanceGameTests::isEchoNpc).size();
    }

    private static boolean isEchoNpc(Entity entity) {
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return ECHO_NPC_ENTITY_TYPE.equals(type);
    }

    private static void mark(String testName) {
        LOGGER.info("ESNPC-GAMETEST-RUN {}", testName);
    }
}
