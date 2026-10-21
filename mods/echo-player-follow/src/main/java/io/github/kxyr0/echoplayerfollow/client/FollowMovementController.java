package io.github.kxyr0.echoplayerfollow.client;

import io.github.kxyr0.echoplayerfollow.shared.FollowConstants;
import io.github.kxyr0.echoplayerfollow.shared.FollowSteering;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

final class FollowMovementController {
    private Vec3 lastGoal;
    private int lastEntityId = -1;
    private boolean sprinting;

    Optional<String> tick(ClientFollowState.ActiveTarget target) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return Optional.of("client_unavailable");
        }
        Entity entity = minecraft.level.getEntity(target.entityId());
        if (entity == null || !entity.isAlive() || entity.isRemoved()) {
            return Optional.of("npc_lost");
        }
        double npcDistance = player.distanceTo(entity);
        if (npcDistance > FollowConstants.CANCEL_DISTANCE) {
            return Optional.of("too_far");
        }
        apply(player, entity, npcDistance);
        return Optional.empty();
    }

    void release() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.input == null) {
            return;
        }
        player.input.forwardImpulse = 0.0F;
        player.input.leftImpulse = 0.0F;
        player.input.up = false;
        player.input.down = false;
        player.input.left = false;
        player.input.right = false;
        player.setSprinting(false);
        sprinting = false;
    }

    private void apply(LocalPlayer player, Entity npc, double npcDistance) {
        Vec3 goal = stableGoal(npc.getId(), followGoal(npc));
        FollowSteering.InputPlan plan = FollowSteering.plan(goal.x - player.getX(), goal.z - player.getZ(),
                player.getYRot(), npcDistance);
        player.input.forwardImpulse = (float) plan.forwardImpulse();
        player.input.leftImpulse = (float) plan.leftImpulse();
        player.input.up = plan.forwardImpulse() > 0.35D;
        player.input.down = plan.forwardImpulse() < -0.35D;
        player.input.left = plan.leftImpulse() > 0.35D;
        player.input.right = plan.leftImpulse() < -0.35D;
        player.setSprinting(sprint(plan, npcDistance));
    }

    private boolean sprint(FollowSteering.InputPlan plan, double npcDistance) {
        if (!plan.moving()) {
            sprinting = false;
            return false;
        }
        if (sprinting && npcDistance <= FollowConstants.SPRINT_STOP_DISTANCE) {
            sprinting = false;
        } else if (!sprinting && plan.sprint()) {
            sprinting = true;
        }
        return sprinting;
    }

    private Vec3 stableGoal(int entityId, Vec3 goal) {
        if (entityId == lastEntityId && lastGoal != null && horizontalDistance(lastGoal, goal)
                < FollowConstants.GOAL_DEADBAND) {
            return lastGoal;
        }
        lastEntityId = entityId;
        lastGoal = goal;
        return goal;
    }

    private Vec3 followGoal(Entity npc) {
        Vec3 forward = horizontal(npc.getDeltaMovement());
        if (forward.lengthSqr() < FollowConstants.NPC_STOP_SPEED * FollowConstants.NPC_STOP_SPEED) {
            forward = horizontal(npc.getLookAngle());
        }
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return npc.position().subtract(forward.normalize().scale(FollowConstants.FOLLOW_DISTANCE));
    }

    private Vec3 horizontal(Vec3 source) {
        return new Vec3(source.x, 0.0D, source.z);
    }

    private double horizontalDistance(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
