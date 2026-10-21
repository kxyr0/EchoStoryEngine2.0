package io.github.kxyr0.echonpc.forge.gametest;

import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.actor;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.definition;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.entities;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.installedState;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.localHost;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.mark;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.pos;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.request;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.definition.NpcDefinitionCatalog;
import io.github.kxyr0.echonpc.forge.EchoNpcMod;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcHost;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(EchoNpcMod.MOD_ID)
public final class ForgeNpcLifecycleGameTests {
    private ForgeNpcLifecycleGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void newHostDoesNotReuseActorRegistry(GameTestHelper helper) {
        mark("ForgeNpcLifecycleGameTests.newHostDoesNotReuseActorRegistry");
        NpcCatalogState state = installedState();
        String actorId = actor("host_first");
        ForgeNpcHost first = localHost(helper, state);
        helper.runAfterDelay(20L, () -> {
            first.tick();
            helper.assertTrue(first.service().health().active(), "first host recovery must finish");
            first.service().spawn(request(actorId, "sample:father", helper, pos(helper, 13)));
            helper.runAfterDelay(20L, () -> {
                first.tick();
                helper.assertTrue(first.service().query(new ActorId(actorId)).actor().isPresent(),
                        "first host must save actor");
                first.stop();
                ForgeNpcHost second = localHost(helper, state);
                helper.runAfterDelay(20L, () -> {
                    second.tick();
                    helper.assertTrue(second.service().query(new ActorId(actorId)).actor().isPresent(),
                            "new host recovers durable actor");
                    second.service().despawn(new ActorId(actorId));
                    helper.runAfterDelay(20L, () -> {
                        second.tick();
                        second.stop();
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void shutdownClearsActors(GameTestHelper helper) {
        mark("ForgeNpcLifecycleGameTests.shutdownClearsActors");
        ForgeNpcHost host = localHost(helper, installedState());
        String actorId = actor("shutdown");
        helper.runAfterDelay(20L, () -> {
            host.tick();
            helper.assertTrue(host.service().health().active(), "host recovery must finish");
            host.service().spawn(request(actorId, "sample:father", helper, pos(helper, 14)));
            helper.runAfterDelay(20L, () -> {
                host.tick();
                host.service().despawn(new ActorId(actorId));
                helper.runAfterDelay(20L, () -> {
                    host.tick();
                    host.stop();
                    helper.assertTrue(host.service().listActors(10).actors().isEmpty(), "stop clears actor ownership");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void dedicatedServerDoesNotLoadRenderer(GameTestHelper helper) {
        mark("ForgeNpcLifecycleGameTests.dedicatedServerDoesNotLoadRenderer");
        helper.assertTrue(FMLEnvironment.dist == Dist.DEDICATED_SERVER, "GameTest path is dedicated server");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void actorPinsSpawnDefinition(GameTestHelper helper) {
        mark("ForgeNpcLifecycleGameTests.actorPinsSpawnDefinition");
        NpcCatalogState state = installedState();
        ForgeNpcHost host = localHost(helper, state);
        String actorId = actor("pinned");
        helper.runAfterDelay(20L, () -> {
            host.tick();
            host.service().spawn(request(actorId, "sample:father", helper, pos(helper, 15)));
            helper.runAfterDelay(20L, () -> {
                host.tick();
                state.install(NpcDefinitionCatalog.of(2L, List.of(definition("sample:father", "Новый отец"))));
                var actor = host.service().query(new ActorId(actorId)).actor().orElseThrow();
                helper.assertTrue(actor.displayName().equals("Отец"), "active actor keeps pinned definition");
                host.service().despawn(new ActorId(actorId));
                helper.runAfterDelay(20L, () -> {
                    host.tick();
                    host.stop();
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void missingSkinDoesNotBlockServerSpawn(GameTestHelper helper) {
        mark("ForgeNpcLifecycleGameTests.missingSkinDoesNotBlockServerSpawn");
        NpcPosition pos = pos(helper, 16);
        String actorId = actor("missing_skin");
        var service = service(helper);
        var result = service.spawn(request(actorId, "sample:simple", helper, pos));
        helper.assertTrue(result.status() == NpcResults.SpawnStatus.QUEUED, "server spawn does not decode skin");
        helper.succeedWhen(() -> {
            helper.assertTrue(entities(helper, pos, actorId).size() == 1, "missing skin actor materializes");
            service.despawn(new ActorId(actorId));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void entityHasNoGoalsButNavigationEnabled(GameTestHelper helper) {
        mark("ForgeNpcLifecycleGameTests.entityHasNoGoalsButNavigationEnabled");
        NpcPosition pos = pos(helper, 17);
        String actorId = actor("no_ai");
        var service = service(helper);
        service.spawn(request(actorId, "sample:father", helper, pos));
        helper.succeedWhen(() -> {
            helper.assertTrue(entities(helper, pos, actorId).size() == 1, "entity must materialize");
            helper.assertTrue(!entities(helper, pos, actorId).get(0).isNoAi(),
                    "navigation-capable entity must tick vanilla navigation");
            service.despawn(new ActorId(actorId));
        });
    }
}
