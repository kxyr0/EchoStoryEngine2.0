package io.github.kxyr0.echonpcinteraction.forge.seat;

import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import io.github.kxyr0.echonpcinteraction.api.result.NpcRideHandoffStrategy;
import io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class NpcRideHandoff {
    private final NpcSeatOccupancyRegistry occupancies;

    public NpcRideHandoff(NpcSeatOccupancyRegistry occupancies) {
        this.occupancies = occupancies;
    }

    public NpcRideHandoffResult transfer(NpcNavigationBody body, NpcInteractionRequest request,
            InteractionEffectSnapshot before, InteractionEffectSnapshot after, ServerPlayer proxy) {
        Optional<Entity> vehicle = after.proxyVehicle(body.level());
        if (vehicle.isEmpty()) {
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.NONE,
                    NpcInteractionFailure.NPC_INTERACTION_RIDE_TRANSFER_UNSUPPORTED, "Proxy vehicle disappeared.");
        }
        if (body.mob().isPassenger()) {
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.NONE,
                    NpcInteractionFailure.NPC_INTERACTION_ECHO_SEAT_OCCUPANCY_CONFLICT,
                    "NPC is already a passenger.");
        }
        Entity observed = vehicle.orElseThrow();
        NpcRideHandoffResult direct = directAttach(body, request, proxy, observed);
        if (direct.success()) {
            return direct;
        }
        if (before.entityIds().contains(observed.getUUID())) {
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.UNSUPPORTED_EXISTING_VEHICLE,
                    NpcInteractionFailure.NPC_INTERACTION_EXISTING_VEHICLE_HANDOFF_UNSUPPORTED,
                    "Existing vehicle cannot be generically transferred.");
        }
        return anchorFallback(body, request, proxy, observed);
    }

    private NpcRideHandoffResult directAttach(NpcNavigationBody body, NpcInteractionRequest request,
            ServerPlayer proxy, Entity vehicle) {
        if (!body.mob().startRiding(vehicle, true) || body.mob().getVehicle() != vehicle) {
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.DIRECT_ATTACH_WHILE_OCCUPIED,
                    NpcInteractionFailure.NPC_INTERACTION_RIDE_DIRECT_ATTACH_REJECTED,
                    "Vehicle rejected direct NPC attach.");
        }
        proxy.stopRiding();
        if (body.mob().getVehicle() != vehicle || !vehicle.hasPassenger(body.mob())) {
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.DIRECT_ATTACH_WHILE_OCCUPIED,
                    NpcInteractionFailure.NPC_INTERACTION_RIDE_DIRECT_ATTACH_REJECTED,
                    "NPC passenger link was not retained.");
        }
        NpcSeatOccupancy occupancy = occupancy(body, request, vehicle.getUUID().toString(), vehicle.getUUID().toString());
        if (!occupancies.reserve(occupancy)) {
            body.mob().stopRiding();
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.DIRECT_ATTACH_WHILE_OCCUPIED,
                    NpcInteractionFailure.NPC_INTERACTION_ECHO_SEAT_OCCUPANCY_CONFLICT,
                    "Seat occupancy is already owned.");
        }
        return new NpcRideHandoffResult(true, NpcRideHandoffStrategy.DIRECT_ATTACH_WHILE_OCCUPIED,
                Optional.empty(), "NPC attached to observed vehicle.", Optional.empty(),
                Optional.of(occupancy.id()), true, true, true);
    }

    private NpcRideHandoffResult anchorFallback(NpcNavigationBody body, NpcInteractionRequest request,
            ServerPlayer proxy, Entity foreignVehicle) {
        if (!transientSeatCandidate(proxy, foreignVehicle)) {
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.ECHO_SEAT_ANCHOR,
                    NpcInteractionFailure.NPC_INTERACTION_TRANSIENT_VEHICLE_UNVERIFIED,
                    "Observed vehicle is not a safe transient seat result.");
        }
        EchoNpcSeatAnchorEntity anchor = EchoNpcSeatAnchorEntity.create(body.level(), request,
                proxy.position(), foreignVehicle.getYRot());
        body.level().addFreshEntity(anchor);
        if (!body.mob().startRiding(anchor, false) || body.mob().getVehicle() != anchor) {
            anchor.discard();
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.ECHO_SEAT_ANCHOR,
                    NpcInteractionFailure.NPC_INTERACTION_ECHO_SEAT_ATTACHMENT_FAILED,
                    "NPC did not attach to Echo seat anchor.");
        }
        proxy.stopRiding();
        boolean foreignClean = cleanupForeign(foreignVehicle);
        NpcSeatOccupancy occupancy = occupancy(body, request, anchor.getUUID().toString(),
                foreignVehicle.getUUID().toString());
        if (!occupancies.reserve(occupancy)) {
            anchor.discard();
            return NpcRideHandoffResult.failed(NpcRideHandoffStrategy.ECHO_SEAT_ANCHOR,
                    NpcInteractionFailure.NPC_INTERACTION_ECHO_SEAT_OCCUPANCY_CONFLICT,
                    "Seat occupancy is already owned.");
        }
        return new NpcRideHandoffResult(true, NpcRideHandoffStrategy.ECHO_SEAT_ANCHOR,
                Optional.empty(), "NPC attached to Echo seat anchor.", Optional.of(anchor.getUUID().toString()),
                Optional.of(occupancy.id()), true, true, foreignClean);
    }

    private boolean transientSeatCandidate(ServerPlayer proxy, Entity vehicle) {
        return vehicle.hasPassenger(proxy) && vehicle.getPassengers().size() == 1
                && vehicle.distanceToSqr(proxy) <= 16.0D;
    }

    private boolean cleanupForeign(Entity vehicle) {
        if (vehicle.getPassengers().isEmpty() && !vehicle.isRemoved()) {
            vehicle.discard();
        }
        return vehicle.isRemoved() || vehicle.getPassengers().isEmpty();
    }

    private NpcSeatOccupancy occupancy(NpcNavigationBody body, NpcInteractionRequest request,
            String anchorId, String vehicleId) {
        return new NpcSeatOccupancy(body.dimensionId(), request.target().position(), request.npcId(),
                request.commandId(), anchorId, vehicleId, body.level().getGameTime());
    }
}
