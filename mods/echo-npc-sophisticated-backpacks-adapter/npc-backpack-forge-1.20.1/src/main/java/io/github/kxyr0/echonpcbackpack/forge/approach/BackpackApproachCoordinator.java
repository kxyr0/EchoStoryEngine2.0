package io.github.kxyr0.echonpcbackpack.forge.approach;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.forge.api.service.EchoNavigationServiceBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Chooses a safe adjacent stand position and delegates all movement to Echo Navigation.
 */
public final class BackpackApproachCoordinator {
    public StartResult start(MinecraftServer server, NpcNavigationBody body, BackpackBlockLocation target,
            NpcBackpackCommandId commandId) {
        Optional<BlockPos> stand = candidates(body.level(), position(target)).stream()
                .min(Comparator.comparingDouble(value -> value.distSqr(body.mob().blockPosition())));
        if (stand.isEmpty()) return new StartResult(Status.BLOCKED, Optional.empty(), "No safe approach position.");
        if (body.mob().distanceToSqr(stand.orElseThrow().getCenter()) <= 1.7D * 1.7D) {
            return new StartResult(Status.ARRIVED, Optional.empty(), "NPC is already in interaction range.");
        }
        var lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.status() != EchoNavigationServiceBridge.Status.RESOLVED) {
            return new StartResult(Status.UNAVAILABLE, Optional.empty(), "Echo Navigation service is unavailable.");
        }
        String suffix = hash(commandId.value() + "@" + target);
        NavigationMoveId moveId = new NavigationMoveId("echo_npc_sophisticated_backpacks:" + suffix);
        long settingsRevision = lookup.queryService().orElseThrow().settings().revision();
        NavigationRequest request = new NavigationRequest(moveId,
                new NavigationCommandId("echo_npc_sophisticated_backpacks:" + suffix),
                new NavigationExecutionId("echo_npc_sophisticated_backpacks:" + suffix),
                new NavigationOwnerId("echo_npc_sophisticated_backpacks:transition"),
                new NavigationSubjectId(body.actorId().value()),
                new NavigationDestination(target.dimension().value(), stand.orElseThrow().getX(),
                        stand.orElseThrow().getY(), stand.orElseThrow().getZ()),
                NavigationBackendId.VANILLA, settingsRevision, 0L, NavigationGait.WALK, 3.2D, true);
        var result = lookup.service().orElseThrow().move(request);
        return result.status() == io.github.kxyr0.echonavigation.forge.api.service.NavigationService.Status.ACCEPTED
                || result.status() == io.github.kxyr0.echonavigation.forge.api.service.NavigationService.Status.DUPLICATE
                ? new StartResult(Status.MOVING, Optional.of(moveId.value()), result.message())
                : new StartResult(Status.BLOCKED, Optional.empty(), result.message());
    }

    public PollResult poll(MinecraftServer server, String moveId) {
        var lookup = EchoNavigationServiceBridge.lookup(server);
        if (lookup.status() != EchoNavigationServiceBridge.Status.RESOLVED) {
            return new PollResult(Status.UNAVAILABLE, "Echo Navigation service is unavailable.");
        }
        var snapshot = lookup.queryService().orElseThrow().move(new NavigationMoveId(moveId));
        if (snapshot.isEmpty()) return new PollResult(Status.BLOCKED, "Navigation move disappeared.");
        NavigationStatus status = snapshot.orElseThrow().status();
        if (status == NavigationStatus.ARRIVED) return new PollResult(Status.ARRIVED, "NPC arrived.");
        if (status == NavigationStatus.BLOCKED || status == NavigationStatus.CANCELLED
                || status == NavigationStatus.FAILED) {
            return new PollResult(Status.BLOCKED, snapshot.orElseThrow().failure()
                    .map(Object::toString).orElse(status.name()));
        }
        return new PollResult(Status.MOVING, status.name());
    }

    private List<BlockPos> candidates(ServerLevel level, BlockPos target) {
        return Direction.Plane.HORIZONTAL.stream().map(target::relative)
                .filter(position -> safe(level, position)).toList();
    }

    private boolean safe(ServerLevel level, BlockPos feet) {
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP);
    }

    private BlockPos position(BackpackBlockLocation location) {
        return new BlockPos(location.position().x(), location.position().y(), location.position().z());
    }

    private String hash(String input) {
        try {
            byte[] value = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(value, 0, 12);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record StartResult(Status status, Optional<String> moveId, String message) { }
    public record PollResult(Status status, String message) { }
    public enum Status { MOVING, ARRIVED, BLOCKED, UNAVAILABLE }
}
