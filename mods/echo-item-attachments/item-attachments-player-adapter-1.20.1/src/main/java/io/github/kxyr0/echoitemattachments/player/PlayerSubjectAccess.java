package io.github.kxyr0.echoitemattachments.player;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.model.WorldPosition;
import io.github.kxyr0.echoitemattachments.forge.api.subject.ApproachStatus;
import io.github.kxyr0.echoitemattachments.forge.api.subject.AttachmentSubjectAccess;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorStatus;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import io.github.kxyr0.echoplayeractor.forge.api.service.EchoPlayerActorServiceBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

final class PlayerSubjectAccess implements AttachmentSubjectAccess {
    private final MinecraftServer server;
    private final ServerPlayer player;
    private final PlayerActorId actorId;

    PlayerSubjectAccess(MinecraftServer server, ServerPlayer player) {
        this.server = server;
        this.player = player;
        this.actorId = PlayerActorId.of(player.getUUID());
    }

    @Override
    public LivingEntity entity() {
        return player;
    }

    @Override
    public boolean authorises(String ownerId) {
        if (ownerId.equals("echo_item_attachments:death")) return true;
        var lookup = EchoPlayerActorServiceBridge.lookup(server);
        if (lookup.status() != EchoPlayerActorServiceBridge.Status.RESOLVED) return false;
        var leases = lookup.leaseService().orElseThrow();
        return leases.active(actorId).isPresent() && leases.authorises(actorId, owner(ownerId));
    }

    @Override
    public ApproachStatus approach(AttachmentCommandId commandId, WorldPosition target,
            double acceptedDistance, int timeoutTicks, String ownerId) {
        if (!authorises(ownerId)) return ApproachStatus.UNAUTHORISED;
        if (!player.level().dimension().location().toString().equals(target.dimensionId())) {
            return ApproachStatus.BLOCKED;
        }
        if (player.distanceToSqr(target.x(), target.y(), target.z())
                <= acceptedDistance * acceptedDistance) return ApproachStatus.ARRIVED;
        var lookup = EchoPlayerActorServiceBridge.lookup(server);
        var request = new PlayerActorMoveRequest(moveId(commandId), actorId, owner(ownerId),
                new Vector3(target.x(), target.y(), target.z()), target.dimensionId(), timeoutTicks);
        var result = lookup.service().orElseThrow().move(request);
        return switch (result.status()) {
            case ACCEPTED, WAITING, RUNNING, SUCCEEDED, DUPLICATE -> ApproachStatus.RUNNING;
            case REJECTED -> ApproachStatus.UNAUTHORISED;
            case FAILED, UNAVAILABLE -> ApproachStatus.BLOCKED;
        };
    }

    @Override
    public void cancelApproach(AttachmentCommandId commandId, String ownerId) {
        var lookup = EchoPlayerActorServiceBridge.lookup(server);
        if (lookup.status() == EchoPlayerActorServiceBridge.Status.RESOLVED && authorises(ownerId)) {
            lookup.service().orElseThrow().stop(actorId, owner(ownerId));
        }
    }

    private PlayerActorOwnerId owner(String value) {
        return new PlayerActorOwnerId(value);
    }

    private PlayerActorCommandId moveId(AttachmentCommandId commandId) {
        java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(commandId.value()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new PlayerActorCommandId("item-attachment-" + uuid);
    }
}
