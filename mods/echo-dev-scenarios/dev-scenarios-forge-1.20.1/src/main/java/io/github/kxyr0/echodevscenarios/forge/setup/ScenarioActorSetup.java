package io.github.kxyr0.echodevscenarios.forge.setup;

import io.github.kxyr0.echodevscenarios.api.ScenarioSessionId;
import io.github.kxyr0.echodevscenarios.api.ScenarioStepId;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupPort.StartResult;
import io.github.kxyr0.echodevscenarios.port.ScenarioSetupRequest;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonpc.api.form.NpcFormCommandId;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormStatus;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeRequest;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlMode;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlLeaseId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerControlScopeId;
import io.github.kxyr0.echoplayeractor.api.request.placement.PlayerActorSpawnRequest;
import io.github.kxyr0.echoplayeractor.forge.api.service.EchoPlayerActorServiceBridge;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

final class ScenarioActorSetup {
    private final ScenarioLocationCatalog locations;
    private final Supplier<ServerPlayer> player;
    private final Map<ScenarioSessionId, PlayerActorOwnerId> owners = new LinkedHashMap<>();
    private ForgeScenarioSetup.Completion completion = (step, succeeded, detail) -> { };
    private Pending pending;

    ScenarioActorSetup(ScenarioLocationCatalog locations, Supplier<ServerPlayer> player) {
        this.locations = locations;
        this.player = player;
    }

    void bind(ForgeScenarioSetup.Completion value) { completion = value; }

    StartResult place(ScenarioSetupRequest request) {
        ScenarioLocation location = locations.location(request.step().arguments().get(0))
                .orElseThrow(() -> new IllegalArgumentException("Location not found"));
        ServerPlayer current = requirePlayer();
        var lookup = EchoPlayerActorServiceBridge.lookup(current.getServer());
        var service = lookup.service().orElseThrow(() -> new IllegalStateException("Player Actor unavailable"));
        PlayerActorOwnerId owner = owners.computeIfAbsent(request.owner(), ignored -> acquire(request, lookup));
        var result = service.spawnAt(new PlayerActorSpawnRequest(
                new PlayerActorCommandId(ForgeScenarioSetup.token(request)), PlayerActorId.of(current.getUUID()),
                owner, new Vector3(location.x(), location.y(), location.z()), location.yaw(), location.pitch()));
        if (result.status() == PlayerActorStatus.SUCCEEDED || result.status() == PlayerActorStatus.DUPLICATE) {
            return StartResult.completed();
        }
        if (result.status() == PlayerActorStatus.ACCEPTED || result.status() == PlayerActorStatus.WAITING
                || result.status() == PlayerActorStatus.RUNNING) {
            return waitFor(request, PendingKind.PLAYER, location.id(), location, null);
        }
        return StartResult.failed(result.message());
    }

    StartResult ensure(ScenarioSetupRequest request) {
        String target = request.step().arguments().get(0);
        ServerPlayer current = requirePlayer();
        var service = EchoNpcServiceBridge.lookup(current.getServer()).service()
                .orElseThrow(() -> new IllegalStateException("Echo NPC unavailable"));
        ActorId actor = new ActorId(target);
        if (service.isMaterialized(actor)) return StartResult.completed();
        var result = service.spawnAt(new SpawnNpcRequest(actor, new NpcDefinitionId(target),
                new NpcWorldId(current.serverLevel().dimension().location().toString()),
                new NpcPosition(current.getX() + npcOffset(target), current.getY(), current.getZ() + 1.5D),
                NpcRotation.zero(), new ActorMutationId(ForgeScenarioSetup.token(request))));
        if (result.status() == NpcResults.SpawnStatus.REJECTED
                || result.status() == NpcResults.SpawnStatus.DEFINITION_NOT_FOUND
                || result.status() == NpcResults.SpawnStatus.FAILURE) return StartResult.failed(result.message());
        return service.isMaterialized(actor) ? StartResult.completed()
                : waitFor(request, PendingKind.NPC, target, null, null);
    }

    StartResult form(ScenarioSetupRequest request) {
        String actor = request.step().arguments().get(0);
        String form = request.step().arguments().get(1);
        var service = EchoNpcServiceBridge.lookup(requirePlayer().getServer()).service()
                .orElseThrow(() -> new IllegalStateException("Echo NPC unavailable"));
        var result = service.setForm(new NpcFormChangeRequest(new ActorId(actor), new NpcFormId(form),
                new NpcFormCommandId(ForgeScenarioSetup.token(request))));
        if (result.status() == NpcFormStatus.COMMITTED || result.status() == NpcFormStatus.ALREADY_ACTIVE) {
            return StartResult.completed();
        }
        return result.status() == NpcFormStatus.QUEUED
                ? waitFor(request, PendingKind.FORM, actor, null, form) : StartResult.failed(result.message());
    }

    void tick() {
        Pending current = pending;
        if (current == null) return;
        boolean ready = switch (current.kind()) {
            case PLAYER -> atLocation(requirePlayer(), current.location());
            case NPC -> npcReady(current.target(), null);
            case FORM -> npcReady(current.target(), current.form());
        };
        if (ready) complete(current, true, "");
        else if (current.remainingTicks() <= 1) complete(current, false, "setup confirmation timed out");
        else pending = current.tick();
    }

    void release(ScenarioSessionId owner) {
        if (pending != null && pending.owner().equals(owner)) pending = null;
        PlayerActorOwnerId controlOwner = owners.remove(owner);
        if (controlOwner == null || player.get() == null) return;
        var lookup = EchoPlayerActorServiceBridge.lookup(player.get().getServer());
        lookup.leaseService().ifPresent(value -> value.release(PlayerActorId.of(player.get().getUUID()), controlOwner));
    }

    private PlayerActorOwnerId acquire(ScenarioSetupRequest request,
            EchoPlayerActorServiceBridge.LookupResult lookup) {
        ServerPlayer current = requirePlayer();
        String token = ForgeScenarioSetup.token(request);
        PlayerActorOwnerId owner = PlayerActorOwnerId.system(token);
        PlayerControlLease lease = new PlayerControlLease(new PlayerControlLeaseId(token),
                PlayerActorId.of(current.getUUID()), PlayerControlScopeId.CINEMATIC_CONTROL,
                PlayerControlMode.CINEMATIC_CONTROL, owner, List.of(), current.getServer().getTickCount(),
                0L, PlayerControlStatus.ACTIVE, 1L);
        var result = lookup.leaseService().orElseThrow().acquire(lease);
        if (result.status() != PlayerActorStatus.SUCCEEDED && result.status() != PlayerActorStatus.DUPLICATE) {
            throw new IllegalStateException(result.message());
        }
        return owner;
    }

    private StartResult waitFor(ScenarioSetupRequest request, PendingKind kind, String target,
            ScenarioLocation location, String form) {
        pending = new Pending(request.owner(), request.step().id(), kind, target, location, form, 200);
        return StartResult.waiting(ForgeScenarioSetup.token(request));
    }

    private boolean npcReady(String actor, String form) {
        var service = EchoNpcServiceBridge.lookup(requirePlayer().getServer()).service().orElse(null);
        if (service == null) return false;
        return form == null ? service.isMaterialized(new ActorId(actor))
                : service.isForm(new ActorId(actor), new NpcFormId(form));
    }

    private void complete(Pending value, boolean succeeded, String detail) {
        pending = null;
        completion.complete(value.step(), succeeded, detail);
    }

    private boolean atLocation(ServerPlayer value, ScenarioLocation location) {
        return value.serverLevel().dimension().location().toString().equals(location.dimension())
                && value.distanceToSqr(location.x(), location.y(), location.z()) < 0.01D;
    }

    private double npcOffset(String id) { return 1.5D + Math.floorMod(id.hashCode(), 3); }
    private ServerPlayer requirePlayer() {
        ServerPlayer value = player.get();
        if (value == null) throw new IllegalStateException("player is unavailable");
        return value;
    }

    private enum PendingKind { PLAYER, NPC, FORM }
    private record Pending(ScenarioSessionId owner, ScenarioStepId step, PendingKind kind, String target,
                           ScenarioLocation location, String form, int remainingTicks) {
        Pending tick() { return new Pending(owner, step, kind, target, location, form, remainingTicks - 1); }
    }
}
