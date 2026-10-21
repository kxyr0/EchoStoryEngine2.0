package io.github.kxyr0.echonpcinteraction.forge.runtime;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionOwnerId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionOutcome;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import io.github.kxyr0.echonpcinteraction.forge.seat.EchoNpcSeatAnchorEntity;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcSeatOccupancyRegistry;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

final class NpcStandUpFlow {
    private final NpcSeatOccupancyRegistry occupancies;

    NpcStandUpFlow(NpcSeatOccupancyRegistry occupancies) {
        this.occupancies = occupancies;
    }

    NpcInteractionResult standUp(MinecraftServer server, ActorId npcId, NpcInteractionOwnerId ownerId) {
        NpcInteractionCommandId commandId = commandId(npcId, ownerId);
        var body = EchoNpcNavigationBodyBridge.lookup(server, npcId).body();
        if (body.isEmpty()) {
            return NpcInteractionResult.failed(commandId, NpcInteractionOutcome.NPC_NOT_MATERIALIZED,
                    NpcInteractionFailure.NPC_INTERACTION_NPC_NOT_MATERIALIZED, "NPC body is not materialized.");
        }
        Optional<Entity> vehicle = Optional.ofNullable(body.orElseThrow().mob().getVehicle());
        vehicle.ifPresent(value -> {
            body.orElseThrow().mob().stopRiding();
            if (value instanceof EchoNpcSeatAnchorEntity) {
                value.discard();
            }
        });
        occupancies.releaseNpc(npcId);
        return new NpcInteractionResult(commandId, NpcInteractionStatus.COMPLETED,
                NpcInteractionOutcome.COMPLETED, Optional.empty(), Optional.empty(), Optional.empty(),
                vehicle.isPresent() ? "NPC stood up." : "NPC was already standing.");
    }

    private NpcInteractionCommandId commandId(ActorId npcId, NpcInteractionOwnerId ownerId) {
        String seed = ownerId.value() + "|" + npcId.value();
        return new NpcInteractionCommandId("echo_npc_interaction:stand_up_"
                + Integer.toUnsignedString(seed.hashCode(), 36));
    }
}
