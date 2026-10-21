package io.github.kxyr0.echonpcinteraction.forge.provider;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionApproach;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionHit;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionPlan;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionReceipt;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NpcInteractionExecutionPort;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProvider;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProviderSelection;
import io.github.kxyr0.echonpcinteraction.forge.api.target.NpcInteractionTargetView;
import io.github.kxyr0.echonpcinteraction.forge.proxy.ProxyUsePipeline;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcSeatOccupancyRegistry;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;

public final class VanillaBlockUseProvider implements NpcInteractionProvider {
    public static final NpcInteractionProviderId ID =
            new NpcInteractionProviderId("echo_npc_interaction:world_use");

    private final ProxyUsePipeline proxyUse;

    public VanillaBlockUseProvider(NpcSeatOccupancyRegistry occupancies) {
        this.proxyUse = new ProxyUsePipeline(occupancies);
    }

    @Override
    public NpcInteractionProviderId id() {
        return ID;
    }

    @Override
    public NpcInteractionProviderSelection inspect(NpcInteractionTargetView target) {
        return NpcInteractionProviderSelection.supported(10, 10);
    }

    @Override
    public NpcInteractionPlan plan(NpcInteractionRequest request, NpcInteractionTargetView target) {
        return new NpcInteractionPlan(ID, target.snapshot(), approaches(request, target),
                "VANILLA_USE", "BLOCK_STATE_CHANGED", 1L);
    }

    @Override
    public NpcInteractionResult execute(MinecraftServer server, NpcInteractionExecutionPort port,
            NpcInteractionRequest request, NpcInteractionPlan plan) {
        var body = EchoNpcNavigationBodyBridge.lookup(server, request.npcId()).body();
        if (body.isEmpty()) {
            return NpcInteractionResult.failed(request.commandId(),
                    io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionOutcome.NPC_NOT_MATERIALIZED,
                    io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure.NPC_INTERACTION_NPC_NOT_MATERIALIZED,
                    "NPC body is not materialized.");
        }
        var use = proxyUse.use(body.orElseThrow(), request, plan.approaches().get(0));
        if (!use.success()) {
            return NpcInteractionResult.failed(request.commandId(), use.outcome(),
                    use.failure().orElseThrow(), use.message());
        }
        NpcInteractionReceipt receipt = new NpcInteractionReceipt(request.commandId(), request.npcId(), ID,
                request.target().position(), plan.targetSnapshot().blockId(), use.beforeFingerprint(),
                use.interactionResult(), use.afterFingerprint(), Optional.empty(), server.overworld().getGameTime(),
                plan.verificationPolicy(), use.outcome(), use.consequences(), use.spawnedEntityIds(),
                use.proxyVehicleBefore(), use.proxyVehicleAfter(), use.rideHandoffStrategy(),
                use.echoSeatAnchorId(), use.seatOccupancyId(), use.npcPassengerConfirmed(),
                use.proxyCleanupSucceeded(), use.foreignVehicleCleanupSucceeded(), 2L, 2L);
        return NpcInteractionResult.completed(receipt, use.message());
    }

    @Override
    public NpcInteractionResult verify(MinecraftServer server, NpcInteractionExecutionPort port,
            NpcInteractionRequest request, NpcInteractionPlan plan) {
        return NpcInteractionResult.accepted(request.commandId(), "Verification is performed during execution.");
    }

    private List<NpcInteractionApproach> approaches(NpcInteractionRequest request, NpcInteractionTargetView target) {
        int x = target.snapshot().position().x();
        int y = target.snapshot().position().y();
        int z = target.snapshot().position().z();
        List<NpcInteractionApproach> all = List.of(
                approach(x + 1.5D, y, z + 0.5D, "WEST", "EAST", x + 1.0D, y + 0.5D, z + 0.5D),
                approach(x - 0.5D, y, z + 0.5D, "EAST", "WEST", x, y + 0.5D, z + 0.5D),
                approach(x + 0.5D, y, z + 1.5D, "NORTH", "SOUTH", x + 0.5D, y + 0.5D, z + 1.0D),
                approach(x + 0.5D, y, z - 0.5D, "SOUTH", "NORTH", x + 0.5D, y + 0.5D, z));
        if ("AUTO".equalsIgnoreCase(request.face())) {
            return all;
        }
        return all.stream().filter(candidate -> candidate.hit().blockFace().equalsIgnoreCase(request.face()))
                .toList();
    }

    private NpcInteractionApproach approach(double sx, int sy, double sz, String facing, String face,
            double hx, double hy, double hz) {
        return new NpcInteractionApproach(new Vector3(sx, sy, sz), facing,
                new NpcInteractionHit(new Vector3(hx, hy, hz), face), 5.0D, 1.0D, true, 1.0D);
    }
}
