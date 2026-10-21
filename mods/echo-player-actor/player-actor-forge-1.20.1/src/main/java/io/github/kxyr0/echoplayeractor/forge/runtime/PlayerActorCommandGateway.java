package io.github.kxyr0.echoplayeractor.forge.runtime;

import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.pose.PlayerActorSnapshot;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorLookRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorRotationRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarItemRequest;
import io.github.kxyr0.echoplayeractor.api.request.hotbar.PlayerHotbarSelectionRequest;
import io.github.kxyr0.echoplayeractor.api.request.placement.PlayerActorSpawnRequest;
import io.github.kxyr0.echoplayeractor.api.request.seat.PlayerSitRequest;
import io.github.kxyr0.echoplayeractor.core.control.PlayerControlLeaseRegistry;
import io.github.kxyr0.echoplayeractor.core.rotation.PlayerLookAtResolver;
import io.github.kxyr0.echoplayeractor.forge.api.motor.PlayerActorMovementBridge;
import io.github.kxyr0.echoplayeractor.forge.api.seat.PlayerActorSeatProvider;
import io.github.kxyr0.echoplayeractor.forge.body.PlayerActorSnapshots;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerBodyResolver;
import io.github.kxyr0.echoplayeractor.forge.inventory.PlayerHotbarCommand;
import io.github.kxyr0.echoplayeractor.forge.interaction.ServerPlayerInteractionPipeline;
import io.github.kxyr0.echoplayeractor.forge.movement.ServerPlayerActorMotor;
import io.github.kxyr0.echoplayeractor.forge.placement.PlayerSpawnCommand;
import io.github.kxyr0.echoplayeractor.forge.rotation.ServerPlayerRotationFlow;
import io.github.kxyr0.echoplayeractor.forge.seat.PlayerSitCommand;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerActorCommandGateway {
    private final PlayerControlLeaseRegistry leases;
    private final ServerPlayerBodyResolver resolver;
    private final PlayerActorSnapshots snapshots;
    private final ServerPlayerActorMotor motor;
    private final ServerPlayerRotationFlow rotations;
    private final ServerPlayerInteractionPipeline interactions;
    private final List<PlayerActorMovementBridge> movementBridges;
    private final PlayerSpawnCommand spawnCommand;
    private final PlayerHotbarCommand hotbarCommand;
    private final PlayerSitCommand sitCommand;
    private final PlayerLookAtResolver lookAtResolver = new PlayerLookAtResolver();
    private long revision;

    public PlayerActorCommandGateway(
            PlayerControlLeaseRegistry leases,
            ServerPlayerBodyResolver resolver,
            PlayerActorSnapshots snapshots,
            ServerPlayerActorMotor motor,
            ServerPlayerRotationFlow rotations,
            ServerPlayerInteractionPipeline interactions,
            List<PlayerActorMovementBridge> movementBridges,
            List<PlayerActorSeatProvider> seatProviders
    ) {
        this.leases = leases;
        this.resolver = resolver;
        this.snapshots = snapshots;
        this.motor = motor;
        this.rotations = rotations;
        this.interactions = interactions;
        this.movementBridges = movementBridges;
        this.spawnCommand = new PlayerSpawnCommand(resolver, this::liveSnapshot);
        this.hotbarCommand = new PlayerHotbarCommand(resolver, this::liveSnapshot);
        this.sitCommand = new PlayerSitCommand(resolver, seatProviders);
    }

    public PlayerActorResult spawnAt(PlayerActorSpawnRequest request) {
        if (!authorises(request.actorId(), request.ownerId())) {
            return ownerRejected();
        }
        return spawnCommand.spawnAt(request);
    }

    public PlayerActorResult setHotbarItem(PlayerHotbarItemRequest request) {
        if (!authorises(request.actorId(), request.ownerId())) {
            return ownerRejected();
        }
        return hotbarCommand.setItem(request);
    }

    public PlayerActorResult selectHotbarSlot(PlayerHotbarSelectionRequest request) {
        if (!authorises(request.actorId(), request.ownerId())) {
            return ownerRejected();
        }
        return hotbarCommand.selectSlot(request);
    }

    public PlayerActorResult sitAt(PlayerSitRequest request) {
        if (!authorises(request.actorId(), request.ownerId())) {
            return ownerRejected();
        }
        return sitCommand.sitAt(request);
    }

    public PlayerActorResult rotate(PlayerActorRotationRequest request) {
        if (!authorises(request.actorId(), request.ownerId())) {
            return ownerRejected();
        }
        return rotations.start(request, motor.isMoving(request.actorId()));
    }

    public PlayerActorResult lookAt(PlayerActorLookRequest request) {
        try {
            Optional<ServerPlayer> player = resolver.resolve(request.actorId());
            if (player.isEmpty()) {
                return offline();
            }
            PlayerActorSnapshot snapshot = liveSnapshot(player.orElseThrow());
            return rotate(lookAtResolver.resolve(snapshot.transform(), request));
        } catch (IllegalArgumentException exception) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_LOOK_TARGET_INVALID,
                    exception.getMessage());
        }
    }

    public PlayerActorResult move(PlayerActorMoveRequest request) {
        if (!authorises(request.actorId(), request.ownerId())) {
            return ownerRejected();
        }
        for (PlayerActorMovementBridge bridge : movementBridges) {
            PlayerActorResult result = bridge.move(request, motor);
            if (result.status() != PlayerActorStatus.UNAVAILABLE) {
                return result;
            }
        }
        return motor.start(request);
    }

    public PlayerActorResult stop(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        if (!authorises(actorId, ownerId)) {
            return ownerRejected();
        }
        for (PlayerActorMovementBridge bridge : movementBridges) {
            PlayerActorResult result = bridge.stop(actorId, motor);
            if (result.status() != PlayerActorStatus.UNAVAILABLE) {
                return result;
            }
        }
        return motor.stop(actorId);
    }

    public PlayerActorResult interact(PlayerActorInteractionRequest request) {
        if (!authorises(request.actorId(), request.ownerId())) {
            return ownerRejected();
        }
        return interactions.interact(request);
    }

    public PlayerActorResult standUp(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        Optional<ServerPlayer> player = resolver.resolve(actorId);
        if (player.isEmpty()) {
            return offline();
        }
        if (!authorises(actorId, ownerId)) {
            return ownerRejected();
        }
        if (player.orElseThrow().isPassenger()) {
            player.orElseThrow().stopRiding();
        }
        if (player.orElseThrow().isPassenger()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_STAND_UP_REJECTED,
                    "Seat rejected stand-up.");
        }
        return PlayerActorResult.success(PlayerActorOutcome.STOOD_UP, liveSnapshot(player.orElseThrow()),
                "Player actor is standing.");
    }

    public PlayerActorResult stopUsingItem(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        Optional<ServerPlayer> player = resolver.resolve(actorId);
        if (player.isEmpty()) {
            return offline();
        }
        if (!authorises(actorId, ownerId)) {
            return ownerRejected();
        }
        player.orElseThrow().stopUsingItem();
        return PlayerActorResult.success(PlayerActorOutcome.ITEM_USE_STOPPED, liveSnapshot(player.orElseThrow()),
                "Player item use stopped.");
    }

    private PlayerActorSnapshot liveSnapshot(ServerPlayer player) {
        PlayerActorId actorId = PlayerActorId.of(player.getUUID());
        Optional<PlayerActorCommandId> active = motor.activeCommand(actorId);
        if (active.isEmpty()) {
            active = rotations.activeCommand(actorId);
        }
        return snapshots.snapshot(player, motor.isMoving(actorId), active, ++revision);
    }

    private boolean authorises(PlayerActorId actorId, PlayerActorOwnerId ownerId) {
        return leases.authorises(actorId, ownerId);
    }

    private PlayerActorResult offline() {
        return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE, "Player actor is offline.");
    }

    private PlayerActorResult ownerRejected() {
        return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_CONTROL_OWNER_MISMATCH,
                "Owner is not authorised by the active player control lease.");
    }
}
