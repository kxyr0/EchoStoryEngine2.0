package io.github.kxyr0.echonpcinteraction.forge.proxy;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionApproach;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionConsequenceKind;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionOutcome;
import io.github.kxyr0.echonpcinteraction.api.result.NpcRideHandoffStrategy;
import io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectJournal;
import io.github.kxyr0.echonpcinteraction.forge.consequence.NpcInteractionConsequenceClassifier;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcRideHandoff;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcRideHandoffResult;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcSeatOccupancyRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class ProxyUsePipeline {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NpcInteractionPlayerContext context = new NpcInteractionPlayerContext();
    private final InteractionEffectJournal journal = new InteractionEffectJournal();
    private final NpcInteractionConsequenceClassifier classifier = new NpcInteractionConsequenceClassifier();
    private final NpcRideHandoff rideHandoff;

    public ProxyUsePipeline(NpcSeatOccupancyRegistry occupancies) {
        this.rideHandoff = new NpcRideHandoff(occupancies);
    }

    public ProxyUseResult use(NpcNavigationBody body, NpcInteractionRequest request, NpcInteractionApproach approach) {
        ServerLevel level = body.level();
        BlockPos pos = new BlockPos(request.target().position().x(), request.target().position().y(),
                request.target().position().z());
        ServerPlayer proxy = context.proxy(level);
        ProxyCleanup.clean(proxy);
        prepare(proxy, body);
        InteractionHand hand = hand(request.hand());
        var before = journal.capture(level, pos, proxy);
        BlockState beforeState = level.getBlockState(pos);
        InteractionResult result = proxy.gameMode.useItemOn(proxy, level, proxy.getItemInHand(hand), hand,
                hit(pos, approach));
        var after = journal.capture(level, pos, proxy);
        List<NpcInteractionConsequenceKind> consequences = classifier.classify(before, after);
        if (consequences.contains(NpcInteractionConsequenceKind.MENU_OPENED)) {
            ProxyCleanup.clean(proxy);
            return failure(result, before, after, consequences, NpcInteractionFailure.NPC_INTERACTION_MENU_UNSUPPORTED,
                    NpcInteractionOutcome.PLAYER_BOUND_UNSUPPORTED, "Player-bound menu interaction rejected.");
        }
        if (consequences.contains(NpcInteractionConsequenceKind.PROXY_STARTED_RIDING)) {
            return riding(body, request, proxy, result, before, after, consequences);
        }
        ProxyCleanup cleanup = ProxyCleanup.clean(proxy);
        if (worldStateChanged(consequences)) {
            body.mob().swing(hand, true);
            return success(result, before, after, consequences, cleanup, "World-state interaction verified.");
        }
        if (consequences.contains(NpcInteractionConsequenceKind.PROXY_RECEIVED_ITEM)) {
            return failure(result, before, after, consequences, NpcInteractionFailure.NPC_INTERACTION_PROXY_ITEM_ONLY,
                    NpcInteractionOutcome.PLAYER_BOUND_UNSUPPORTED, "Interaction only mutated proxy inventory.");
        }
        if (!result.consumesAction()) {
            return failure(result, before, after, consequences, NpcInteractionFailure.NPC_INTERACTION_REJECTED,
                    NpcInteractionOutcome.INTERACTION_REJECTED, "Minecraft use pipeline rejected interaction.");
        }
        return failure(result, before, after, consequences,
                NpcInteractionFailure.NPC_INTERACTION_RESULT_UNVERIFIED,
                NpcInteractionOutcome.RESULT_UNVERIFIED, "No useful consequence was verified.");
    }

    private ProxyUseResult riding(NpcNavigationBody body, NpcInteractionRequest request, ServerPlayer proxy,
            InteractionResult result, io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot before,
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot after,
            List<NpcInteractionConsequenceKind> consequences) {
        LOGGER.info("NPC_INTERACTION_PROXY_STARTED_RIDING commandId={} npc={}",
                request.commandId().value(), request.npcId().value());
        NpcRideHandoffResult handoff = rideHandoff.transfer(body, request, before, after, proxy);
        ProxyCleanup cleanup = ProxyCleanup.clean(proxy);
        if (!handoff.success()) {
            LOGGER.warn("NPC_INTERACTION_RIDE_HANDOFF_FAILED commandId={} npc={} strategy={} failure={} message={}",
                    request.commandId().value(), request.npcId().value(), handoff.strategy(),
                    handoff.failure().map(Enum::name).orElse("none"), handoff.message());
            return handoffFailure(result, before, after, consequences, handoff, cleanup);
        }
        LOGGER.info("NPC_INTERACTION_RIDE_HANDOFF_COMPLETED commandId={} npc={} strategy={} occupancy={} anchor={}",
                request.commandId().value(), request.npcId().value(), handoff.strategy(),
                handoff.occupancyId().orElse("none"), handoff.anchorId().orElse("none"));
        body.mob().swing(hand(request.hand()), true);
        return new ProxyUseResult(true, result.name(), before.targetStateFingerprint(),
                after.targetStateFingerprint(), Optional.empty(), NpcInteractionOutcome.COMPLETED, consequences,
                spawned(before, after), before.proxyVehicleId().map(Object::toString),
                after.proxyVehicleId().map(Object::toString), Optional.of(handoff.strategy()),
                handoff.anchorId(), handoff.occupancyId(), handoff.npcPassengerConfirmed(),
                cleanupSucceeded(cleanup), handoff.foreignVehicleCleanupSucceeded(), handoff.message());
    }

    private ProxyUseResult success(InteractionResult result,
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot before,
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot after,
            List<NpcInteractionConsequenceKind> consequences, ProxyCleanup cleanup, String message) {
        return new ProxyUseResult(true, result.name(), before.targetStateFingerprint(), after.targetStateFingerprint(),
                Optional.empty(), NpcInteractionOutcome.COMPLETED, consequences, spawned(before, after),
                before.proxyVehicleId().map(Object::toString), after.proxyVehicleId().map(Object::toString),
                Optional.of(NpcRideHandoffStrategy.NONE), Optional.empty(), Optional.empty(), false,
                cleanupSucceeded(cleanup), true, message);
    }

    private ProxyUseResult handoffFailure(InteractionResult result,
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot before,
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot after,
            List<NpcInteractionConsequenceKind> consequences, NpcRideHandoffResult handoff, ProxyCleanup cleanup) {
        return new ProxyUseResult(false, result.name(), before.targetStateFingerprint(),
                after.targetStateFingerprint(), handoff.failure(), NpcInteractionOutcome.PLAYER_BOUND_UNSUPPORTED,
                consequences, spawned(before, after), before.proxyVehicleId().map(Object::toString),
                after.proxyVehicleId().map(Object::toString), Optional.of(handoff.strategy()),
                handoff.anchorId(), handoff.occupancyId(), handoff.npcPassengerConfirmed(),
                cleanupSucceeded(cleanup), handoff.foreignVehicleCleanupSucceeded(), handoff.message());
    }

    private ProxyUseResult failure(InteractionResult result,
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot before,
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot after,
            List<NpcInteractionConsequenceKind> consequences, NpcInteractionFailure failure,
            NpcInteractionOutcome outcome, String message) {
        return new ProxyUseResult(false, result.name(), before.targetStateFingerprint(), after.targetStateFingerprint(),
                Optional.of(failure), outcome, consequences, spawned(before, after),
                before.proxyVehicleId().map(Object::toString), after.proxyVehicleId().map(Object::toString),
                Optional.of(NpcRideHandoffStrategy.NONE), Optional.empty(), Optional.empty(), false, true, true,
                message);
    }

    private boolean worldStateChanged(List<NpcInteractionConsequenceKind> consequences) {
        return consequences.contains(NpcInteractionConsequenceKind.BLOCK_STATE_CHANGED)
                || consequences.contains(NpcInteractionConsequenceKind.BLOCK_ENTITY_CHANGED)
                || consequences.contains(NpcInteractionConsequenceKind.NEARBY_BLOCK_STATE_CHANGED)
                || consequences.contains(NpcInteractionConsequenceKind.WORLD_EVENT_CONFIRMED)
                || consequences.contains(NpcInteractionConsequenceKind.ENTITY_SPAWNED);
    }

    private void prepare(ServerPlayer proxy, NpcNavigationBody body) {
        proxy.teleportTo(body.level(), body.mob().getX(), body.mob().getY(), body.mob().getZ(),
                body.mob().getYRot(), body.mob().getXRot());
        proxy.setYHeadRot(body.mob().getYHeadRot());
    }

    private BlockHitResult hit(BlockPos pos, NpcInteractionApproach approach) {
        Direction face = Direction.valueOf(approach.hit().blockFace().toUpperCase(Locale.ROOT));
        Vec3 hitPoint = new Vec3(approach.hit().hitPoint().x(), approach.hit().hitPoint().y(),
                approach.hit().hitPoint().z());
        return new BlockHitResult(hitPoint, face, pos, false);
    }

    private InteractionHand hand(String value) {
        return "OFF_HAND".equalsIgnoreCase(value) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private Optional<String> spawned(
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot before,
            io.github.kxyr0.echonpcinteraction.forge.consequence.InteractionEffectSnapshot after) {
        String ids = after.spawnedSince(before).stream().map(Object::toString)
                .collect(java.util.stream.Collectors.joining(","));
        return ids.isBlank() ? Optional.empty() : Optional.of(ids);
    }

    private boolean cleanupSucceeded(ProxyCleanup cleanup) {
        return !cleanup.openedMenu() && !cleanup.proxyWasPassenger();
    }
}
