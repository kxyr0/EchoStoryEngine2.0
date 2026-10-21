package io.github.kxyr0.echoplayeractor.forge.interaction;

import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorFailure;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorOutcome;
import io.github.kxyr0.echoplayeractor.api.command.PlayerActorResult;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionRequest;
import io.github.kxyr0.echoplayeractor.api.request.PlayerActorInteractionResult;
import io.github.kxyr0.echoplayeractor.core.interaction.PlayerInteractionPlan;
import io.github.kxyr0.echoplayeractor.core.interaction.PlayerInteractionPlanner;
import io.github.kxyr0.echoplayeractor.forge.body.PlayerActorSnapshots;
import io.github.kxyr0.echoplayeractor.forge.body.ServerPlayerBodyResolver;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ServerPlayerInteractionPipeline {
    private final ServerPlayerBodyResolver resolver;
    private final PlayerActorSnapshots snapshots;
    private final PlayerInteractionPlanner planner = new PlayerInteractionPlanner();
    private long revision;

    public ServerPlayerInteractionPipeline(ServerPlayerBodyResolver resolver, PlayerActorSnapshots snapshots) {
        this.resolver = resolver;
        this.snapshots = snapshots;
    }

    public PlayerActorResult interact(PlayerActorInteractionRequest request) {
        Optional<ServerPlayer> resolved = resolver.resolve(request.actorId());
        if (resolved.isEmpty()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_OFFLINE, "Player actor is offline.");
        }
        ServerPlayer player = resolved.orElseThrow();
        PlayerInteractionPlan plan = planner.plan(request, eye(player), hasLineOfSight(player, request));
        Optional<PlayerActorFailure> rejected = rejected(plan);
        if (rejected.isPresent()) {
            return PlayerActorResult.rejected(rejected.orElseThrow(), "Player interaction preflight rejected.");
        }
        return dispatch(player, plan);
    }

    private PlayerActorResult dispatch(ServerPlayer player, PlayerInteractionPlan plan) {
        BlockPos pos = new BlockPos(plan.request().x(), plan.request().y(), plan.request().z());
        ServerLevel level = player.serverLevel();
        if (level.getBlockState(pos).isAir()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_INTERACTION_TARGET_INVALID,
                    "Interaction target block is missing.");
        }
        Direction face = plan.request().face().map(value -> Direction.byName(value.toLowerCase()))
                .orElse(Direction.UP);
        if (face == null) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_INTERACTION_FACE_UNRESOLVED,
                    "Interaction face is invalid.");
        }
        InteractionHand hand = hand(plan.request().hand());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        InteractionResult result = player.gameMode.useItemOn(player, level, player.getItemInHand(hand), hand, hit);
        if (!result.consumesAction()) {
            return PlayerActorResult.rejected(PlayerActorFailure.PLAYER_ACTOR_INTERACTION_REJECTED,
                    "Server interaction pipeline rejected the action.");
        }
        player.swing(hand, true);
        PlayerActorInteractionResult receipt = receipt(plan, hand, face);
        return PlayerActorResult.success(PlayerActorOutcome.INTERACTED,
                snapshots.snapshot(player, false, Optional.empty(), ++revision), receipt.message());
    }

    private Optional<PlayerActorFailure> rejected(PlayerInteractionPlan plan) {
        if (!plan.withinReach()) {
            return Optional.of(PlayerActorFailure.PLAYER_ACTOR_INTERACTION_TOO_FAR);
        }
        if (!plan.lineOfSight()) {
            return Optional.of(PlayerActorFailure.PLAYER_ACTOR_INTERACTION_NO_LINE_OF_SIGHT);
        }
        return Optional.empty();
    }

    private boolean hasLineOfSight(ServerPlayer player, PlayerActorInteractionRequest request) {
        BlockPos pos = new BlockPos(request.x(), request.y(), request.z());
        Vec3 from = player.getEyePosition();
        Vec3 to = Vec3.atCenterOf(pos);
        HitResult hit = player.serverLevel().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && ((BlockHitResult) hit).getBlockPos().equals(pos);
    }

    private Vector3 eye(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        return new Vector3(eye.x, eye.y, eye.z);
    }

    private InteractionHand hand(String value) {
        return "OFF_HAND".equalsIgnoreCase(value) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private PlayerActorInteractionResult receipt(
            PlayerInteractionPlan plan,
            InteractionHand hand,
            Direction face
    ) {
        String target = plan.request().x() + "," + plan.request().y() + "," + plan.request().z();
        return new PlayerActorInteractionResult(true, target, hand.name(), Optional.of(face.getName()),
                plan.distance(), plan.lineOfSight(), Optional.of(plan.receiptKey()), "Player interaction accepted.");
    }
}
