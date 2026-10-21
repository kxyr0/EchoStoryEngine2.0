package io.github.kxyr0.echoplayeractor.forge.runtime;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.control.PlayerControlLease;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorRotationRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarItemRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarSelectionRequest;
import io.github.kxyr0.echoplayeractor.api.request.placement.PlayerActorSpawnRequest;
import io.github.kxyr0.echoplayeractor.api.request.seat.PlayerSitRequest;
import io.github.kxyr0.echoplayeractor.core.control.PlayerControlLeaseRegistry;
import io.github.kxyr0.echoplayeractor.forge.api.motor.PlayerActorMovementBridge;
import io.github.kxyr0.echoplayeractor.forge.api.motor.PlayerActorMovementBridgeRegistrationEvent;
import io.github.kxyr0.echoplayeractor.forge.api.seat.PlayerActorSeatProvider;
import io.github.kxyr0.echoplayeractor.forge.api.seat.PlayerActorSeatProviderRegistrationEvent;
import io.github.kxyr0.echoplayeractor.forge.api.service.PlayerActorQueryService;
import io.github.kxyr0.echoplayeractor.forge.api.service.PlayerActorService;
import io.github.kxyr0.echoplayeractor.forge.api.service.PlayerControlLeaseService;
import io.github.kxyr0.echoplayeractor.forge.api.view.PlayerActorSnapshotView;
import io.github.kxyr0.echoplayeractor.forge.body.PlayerActorSnapshots;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerBodyResolver;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerSnapshotView;
import io.github.kxyr0.echoplayeractor.forge.interaction.ServerPlayerInteractionPipeline;
import io.github.kxyr0.echoplayeractor.forge.movement.ServerPlayerActorMotor;
import io.github.kxyr0.echoplayeractor.forge.rotation.ServerPlayerRotationFlow;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

public final class ServerPlayerActorHost implements PlayerActorService, PlayerActorQueryService,
        PlayerControlLeaseService {
    private final PlayerControlLeaseRegistry leases = new PlayerControlLeaseRegistry();
    private final PlayerActorSnapshots snapshots = new PlayerActorSnapshots();
    private final List<PlayerActorMovementBridge> movementBridges = new ArrayList<>();
    private final List<PlayerActorSeatProvider> seatProviders = new ArrayList<>();
    private MinecraftServer server;
    private ServerPlayerBodyResolver resolver;
    private ServerPlayerActorMotor motor;
    private ServerPlayerRotationFlow rotations;
    private PlayerActorCommandGateway gateway;
    private long revision;

    public void start(MinecraftServer startedServer) {
        server = startedServer;
        resolver = new ServerPlayerBodyResolver(startedServer);
        motor = new ServerPlayerActorMotor(resolver, snapshots);
        rotations = new ServerPlayerRotationFlow(resolver, snapshots);
        var interactions = new ServerPlayerInteractionPipeline(resolver, snapshots);
        gateway = new PlayerActorCommandGateway(leases, resolver, snapshots, motor, rotations,
                interactions, movementBridges, seatProviders);
        MinecraftForge.EVENT_BUS.post(new PlayerActorMovementBridgeRegistrationEvent(startedServer,
                this::registerMovementBridge));
        MinecraftForge.EVENT_BUS.post(new PlayerActorSeatProviderRegistrationEvent(startedServer,
                this::registerSeatProvider));
    }

    public void stop() {
        server = null;
        resolver = null;
        motor = null;
        rotations = null;
        gateway = null;
        movementBridges.clear();
        seatProviders.clear();
    }

    public boolean started(MinecraftServer candidate) {
        return server != null && server == candidate;
    }

    public void tick(long serverTick) {
        if (motor != null && rotations != null) {
            motor.tick();
            rotations.tick(false);
        }
    }

    @Override
    public PlayerActorResult spawnAt(PlayerActorSpawnRequest request) {
        return gateway.spawnAt(request);
    }

    @Override
    public PlayerActorResult setHotbarItem(PlayerHotbarItemRequest request) {
        return gateway.setHotbarItem(request);
    }

    @Override
    public PlayerActorResult selectHotbarSlot(PlayerHotbarSelectionRequest request) {
        return gateway.selectHotbarSlot(request);
    }

    @Override
    public PlayerActorResult sitAt(PlayerSitRequest request) {
        return gateway.sitAt(request);
    }

    @Override
    public PlayerActorResult rotate(PlayerActorRotationRequest request) {
        return gateway.rotate(request);
    }

    @Override
    public PlayerActorResult lookAt(PlayerActorLookRequest request) {
        return gateway.lookAt(request);
    }

    @Override
    public PlayerActorResult move(PlayerActorMoveRequest request) {
        return gateway.move(request);
    }

    @Override
    public PlayerActorResult stop(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        return gateway.stop(actorId, ownerId);
    }

    @Override
    public PlayerActorResult interact(PlayerActorInteractionRequest request) {
        return gateway.interact(request);
    }

    @Override
    public PlayerActorResult standUp(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        return gateway.standUp(actorId, ownerId);
    }

    @Override
    public PlayerActorResult stopUsingItem(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        return gateway.stopUsingItem(actorId, ownerId);
    }

    @Override
    public Optional<PlayerActorSnapshotView> snapshot(PlayerActorId actorId) {
        return resolved(actorId).map(player -> new ServerPlayerSnapshotView(liveSnapshot(player)));
    }

    @Override
    public boolean isMoving(PlayerActorId actorId) {
        return motor != null && motor.isMoving(actorId);
    }

    @Override
    public boolean isSitting(PlayerActorId actorId) {
        return resolved(actorId).map(ServerPlayer::isPassenger).orElse(false);
    }

    @Override
    public boolean isUsingItem(PlayerActorId actorId) {
        return resolved(actorId).map(ServerPlayer::isUsingItem).orElse(false);
    }

    @Override
    public int selectedHotbarSlot(PlayerActorId actorId) {
        return resolved(actorId).map(player -> player.getInventory().selected + 1).orElse(0);
    }

    @Override
    public PlayerActorResult acquire(PlayerControlLease lease) {
        return leases.acquire(lease);
    }

    @Override
    public PlayerActorResult release(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        return leases.release(actorId, ownerId);
    }

    @Override
    public void emergencyRelease(PlayerActorId actorId) {
        leases.emergencyRelease(actorId);
        if (motor != null && motor.isMoving(actorId)) {
            motor.stop(actorId);
        }
    }

    @Override
    public boolean authorises(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        return leases.authorises(actorId, ownerId);
    }

    @Override
    public Optional<PlayerControlLease> active(PlayerActorId actorId) {
        return leases.active(actorId);
    }

    private boolean registerMovementBridge(PlayerActorMovementBridge bridge) {
        movementBridges.add(bridge);
        return true;
    }

    private boolean registerSeatProvider(PlayerActorSeatProvider provider) {
        seatProviders.add(provider);
        return true;
    }

    private Optional<ServerPlayer> resolved(PlayerActorId actorId) {
        return resolver == null ? Optional.empty() : resolver.resolve(actorId);
    }

    private io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot liveSnapshot(ServerPlayer player) {
        PlayerActorId actorId = PlayerActorId.of(player.getUUID());
        Optional<PlayerActorCommandId> active = motor == null ? Optional.empty() : motor.activeCommand(actorId);
        if (active.isEmpty() && rotations != null) {
            active = rotations.activeCommand(actorId);
        }
        boolean moving = motor != null && motor.isMoving(actorId);
        return snapshots.snapshot(player, moving, active, ++revision);
    }
}
