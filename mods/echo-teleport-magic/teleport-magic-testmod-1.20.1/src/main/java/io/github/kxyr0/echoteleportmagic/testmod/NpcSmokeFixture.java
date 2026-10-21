package io.github.kxyr0.echoteleportmagic.testmod;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

final class NpcSmokeFixture {
    private static final ActorId ACTOR = new ActorId("echo_teleport_magic_test:nolan");
    private NpcSmokeFixture() { }
    static void spawn(MinecraftServer server) {
        var result = EchoNpcServiceBridge.lookup(server).service().orElseThrow().spawn(new SpawnNpcRequest(ACTOR,
                new NpcDefinitionId(ACTOR.value()), new NpcWorldId("minecraft:overworld"),
                new NpcPosition(0, -60, 0), new NpcRotation(180, 0)));
        if (result.status() != io.github.kxyr0.echonpc.api.service.NpcResults.SpawnStatus.QUEUED
                && result.status() != io.github.kxyr0.echonpc.api.service.NpcResults.SpawnStatus.CREATED) {
            throw new IllegalStateException(result.toString());
        }
    }
    static Optional<LivingEntity> find(MinecraftServer server) {
        return EchoNpcNavigationBodyBridge.lookup(server, ACTOR).body().map(body -> body.mob());
    }
    static void seat(MinecraftServer server, LivingEntity npc) {
        var anchor = new net.minecraft.world.entity.decoration.ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, server.overworld());
        var data = new net.minecraft.nbt.CompoundTag(); data.putBoolean("Marker", true); anchor.load(data);
        anchor.moveTo(0, -60, 0, 180, 0); anchor.setInvisible(true); anchor.setNoGravity(true);
        server.overworld().addFreshEntity(anchor);
        if (!npc.startRiding(anchor, true)) throw new IllegalStateException("Cannot mount NPC fixture");
    }
    static void assertPosition(MinecraftServer server, double x) {
        var view = EchoNpcServiceBridge.lookup(server).service().orElseThrow().query(ACTOR).actor().orElseThrow();
        if (Math.abs(view.position().x() - x) > .01) throw new IllegalStateException("NPC canonical position was not committed");
    }
}
