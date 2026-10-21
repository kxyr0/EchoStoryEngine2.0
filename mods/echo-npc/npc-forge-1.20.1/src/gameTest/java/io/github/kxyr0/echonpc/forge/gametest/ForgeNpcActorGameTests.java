package io.github.kxyr0.echonpc.forge.gametest;

import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.entities;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.mark;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.pos;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.request;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.service;
import static io.github.kxyr0.echonpc.forge.gametest.ForgeNpcGameTestSupport.actor;

import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeStatus;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.forge.EchoNpcMod;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.host.ForgeWorldIds;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(EchoNpcMod.MOD_ID)
public final class ForgeNpcActorGameTests {
    private ForgeNpcActorGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void duplicateSpawnDoesNotCreateSecondEntity(GameTestHelper helper) {
        mark("ForgeNpcActorGameTests.duplicateSpawnDoesNotCreateSecondEntity");
        NpcPosition pos = pos(helper, 4);
        NpcService service = service(helper);
        String actorId = actor("duplicate");
        service.spawn(request(actorId, "sample:father", helper, pos));
        helper.succeedWhen(() -> {
            helper.assertTrue(entities(helper, pos, actorId).size() == 1, "first spawn must materialize");
            var second = service.spawn(request(actorId, "sample:father", helper, pos));
            helper.assertTrue(second.status() == NpcResults.SpawnStatus.ALREADY_ACTIVE, "duplicate is idempotent");
            helper.assertTrue(entities(helper, pos, actorId).size() == 1, "duplicate must not spawn second entity");
            service.despawn(new ActorId(actorId));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void actorIdConflictIsRejected(GameTestHelper helper) {
        mark("ForgeNpcActorGameTests.actorIdConflictIsRejected");
        NpcPosition pos = pos(helper, 5);
        NpcService service = service(helper);
        String actorId = actor("conflict");
        service.spawn(request(actorId, "sample:father", helper, pos));
        helper.succeedWhen(() -> {
            helper.assertTrue(entities(helper, pos, actorId).size() == 1, "first spawn must materialize");
            var conflict = service.spawn(request(actorId, "sample:simple", helper, pos));
            helper.assertTrue(conflict.status() == NpcResults.SpawnStatus.ACTOR_ID_CONFLICT, "conflict is explicit");
            service.despawn(new ActorId(actorId));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void twoActorIdsMayUseOneDefinition(GameTestHelper helper) {
        mark("ForgeNpcActorGameTests.twoActorIdsMayUseOneDefinition");
        NpcService service = service(helper);
        NpcPosition first = pos(helper, 6);
        NpcPosition second = pos(helper, 8);
        String firstActor = actor("multi_one");
        String secondActor = actor("multi_two");
        service.spawn(request(firstActor, "sample:father", helper, first));
        service.spawn(request(secondActor, "sample:father", helper, second));
        helper.succeedWhen(() -> {
            helper.assertTrue(entities(helper, first, firstActor).size() == 1, "first actor entity exists");
            helper.assertTrue(entities(helper, second, secondActor).size() == 1, "second actor entity exists");
            service.despawn(new ActorId(firstActor));
            service.despawn(new ActorId(secondActor));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void teleportMovesEntity(GameTestHelper helper) {
        mark("ForgeNpcActorGameTests.teleportMovesEntity");
        NpcService service = service(helper);
        NpcPosition start = pos(helper, 9);
        NpcPosition target = pos(helper, 11);
        String actorId = actor("teleport");
        service.spawn(request(actorId, "sample:father", helper, start));
        helper.runAfterDelay(20L, () -> {
            helper.assertTrue(entities(helper, start, actorId).size() == 1, "spawn must materialize before teleport");
            var moved = service.teleport(new TeleportNpcRequest(new ActorId(actorId),
                    ForgeWorldIds.fromLevel(helper.getLevel()), target, NpcRotation.zero()));
            helper.assertTrue(moved.status() == NpcResults.TeleportStatus.QUEUED, "teleport must be queued");
        });
        helper.runAfterDelay(40L, () -> {
            helper.assertTrue(entities(helper, target, actorId).size() == 1, "entity must be at target position");
            service.despawn(new ActorId(actorId));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void changeSkinUpdatesDurableActorAndLiveEntity(GameTestHelper helper) {
        mark("ForgeNpcActorGameTests.changeSkinUpdatesDurableActorAndLiveEntity");
        NpcService service = service(helper);
        NpcPosition position = pos(helper, 10);
        String actorId = actor("change_skin");
        NpcSkinId skinId = new NpcSkinId("sample:father/backpack");
        service.spawn(request(actorId, "sample:father", helper, position));
        helper.runAfterDelay(20L, () -> {
            var result = service.changeSkin(new NpcSkinChangeRequest(new ActorId(actorId), skinId));
            helper.assertTrue(result.status() == NpcSkinChangeStatus.QUEUED, "skin change must be queued");
        });
        helper.runAfterDelay(40L, () -> {
            helper.assertTrue(entities(helper, position, actorId).get(0).skinIdValue().equals(skinId.value()),
                    "live entity must receive the new skin ID");
            helper.assertTrue(service.query(new ActorId(actorId)).actor().orElseThrow().skinId()
                    .filter(skinId::equals).isPresent(), "durable actor view must expose the new skin ID");
            service.despawn(new ActorId(actorId));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void despawnRemovesEntity(GameTestHelper helper) {
        mark("ForgeNpcActorGameTests.despawnRemovesEntity");
        NpcPosition pos = pos(helper, 12);
        NpcService service = service(helper);
        String actorId = actor("despawn");
        service.spawn(request(actorId, "sample:father", helper, pos));
        helper.runAfterDelay(20L, () -> {
            helper.assertTrue(entities(helper, pos, actorId).size() == 1, "spawn must materialize before despawn");
            var result = service.despawn(new ActorId(actorId));
            helper.assertTrue(result.status() == NpcResults.DespawnStatus.QUEUED, "despawn must be queued");
        });
        helper.runAfterDelay(40L, () -> {
            helper.assertTrue(entities(helper, pos, actorId).isEmpty(), "entity must be removed");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void repeatedDespawnIsIdempotent(GameTestHelper helper) {
        mark("ForgeNpcActorGameTests.repeatedDespawnIsIdempotent");
        var result = service(helper).despawn(new ActorId(actor("absent")));
        helper.assertTrue(result.status() == NpcResults.DespawnStatus.ALREADY_ABSENT, "absent actor is explicit");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void serviceLookupResolvesHost(GameTestHelper helper) {
        mark("ForgeNpcActorGameTests.serviceLookupResolvesHost");
        var result = EchoNpcServiceBridge.lookup(helper.getLevel().getServer());
        helper.assertTrue(result.status().name().equals("RESOLVED"), "public bridge must resolve current host");
        helper.succeed();
    }
}
