package io.github.kxyr0.echoteleportmagic.forge.integration;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecution;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** Echo NPC keeps ownership of durable actor mutation; no direct writes into its entity/state. */
public final class NpcTeleportCommit {
    private final MinecraftServer server;
    private final Map<UUID, ActorId> actors = new HashMap<>();
    public NpcTeleportCommit(MinecraftServer server) { this.server = server; }

    public void prepare(TeleportExecution execution) {
        var entity = io.github.kxyr0.echoteleportmagic.forge.server.TeleportSubjects.require(server, execution.subject());
        if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("echo_npc")) return;
        var service = EchoNpcServiceBridge.lookup(server).service().orElseThrow();
        if (execution.subjectReference().contains(":")) {
            ActorId actor = new ActorId(execution.subjectReference());
            var view = service.query(actor).actor().orElseThrow();
            var body = EchoNpcNavigationBodyBridge.lookup(server, actor).body().orElseThrow();
            if (!body.mob().getUUID().equals(execution.subject()) || view.restAttachment().isPresent()) {
                throw new IllegalArgumentException("NPC identity mismatch or sleeping actor");
            }
            actors.put(execution.id().value(), actor);
            return;
        }
        var catalog = service.listActors(100_000);
        if (catalog.truncated()) throw new IllegalStateException("NPC catalog truncated; explicit identity resolution required");
        for (var actor : catalog.actors()) {
            var body = EchoNpcNavigationBodyBridge.lookup(server, actor.actorId()).body();
            if (body.isPresent() && body.get().mob().getUUID().equals(execution.subject())) {
                if (actor.restAttachment().isPresent()) throw new IllegalArgumentException("Sleeping NPC cannot teleport");
                actors.put(execution.id().value(), actor.actorId());
                return;
            }
        }
        throw new IllegalStateException("Echo NPC logical actor not found");
    }
    public boolean supports(TeleportExecution execution) { return actors.containsKey(execution.id().value()); }
    public boolean commit(TeleportExecution execution) {
        var service = EchoNpcServiceBridge.lookup(server).service().orElseThrow();
        var d = execution.destination();
        var result = service.teleport(new TeleportNpcRequest(actors.get(execution.id().value()),
                new NpcWorldId(d.dimension()), new NpcPosition(d.x(), d.y(), d.z()), new NpcRotation(d.yaw(), d.pitch()),
                new ActorMutationId("echo_teleport_magic:" + execution.id().value())));
        if (result.status() == NpcResults.TeleportStatus.QUEUED) return false;
        if (result.status() != NpcResults.TeleportStatus.MOVED) throw new IllegalStateException(result.message());
        return true;
    }
    public void release(TeleportExecution e) { actors.remove(e.id().value()); }
}
