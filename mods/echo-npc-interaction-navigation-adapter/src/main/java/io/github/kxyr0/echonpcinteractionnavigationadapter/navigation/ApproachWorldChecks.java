package io.github.kxyr0.echonpcinteractionnavigationadapter.navigation;

import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpcinteraction.api.plan.NpcInteractionApproach;
import io.github.kxyr0.echonpcinteraction.api.request.NpcInteractionRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class ApproachWorldChecks {
    boolean usable(NpcNavigationBody body, NpcInteractionRequest request, NpcInteractionApproach approach) {
        return rejectionReason(body, request, approach).equals("OK");
    }

    String rejectionReason(NpcNavigationBody body, NpcInteractionRequest request, NpcInteractionApproach approach) {
        BlockPos feet = BlockPos.containing(approach.standingPosition().x(), approach.standingPosition().y(),
                approach.standingPosition().z());
        if (!body.dimensionId().equals(request.target().dimensionId())) {
            return "NPC_INTERACTION_APPROACH_WRONG_DIMENSION npcDimension=" + body.dimensionId();
        }
        if (supportMissing(body, feet)) {
            return "NPC_INTERACTION_APPROACH_NO_SUPPORT feet=" + feet;
        }
        if (!body.level().getBlockState(feet).getCollisionShape(body.level(), feet).isEmpty()) {
            return "NPC_INTERACTION_APPROACH_COLLISION feet=" + feet;
        }
        if (!body.level().getBlockState(feet.above()).getCollisionShape(body.level(), feet.above()).isEmpty()) {
            return "NPC_INTERACTION_APPROACH_NO_HEADROOM head=" + feet.above();
        }
        if (!bodyClear(body, approach)) {
            return "NPC_INTERACTION_APPROACH_BODY_COLLISION box=" + bodyBox(body, approach);
        }
        double reach = reachDistance(body, approach);
        if (reach > approach.requiredReach()) {
            return "NPC_INTERACTION_APPROACH_OUT_OF_REACH distance=" + reach;
        }
        if (approach.lineOfSightRequired() && !lineOfSight(body, request, approach)) {
            return "NPC_INTERACTION_APPROACH_LINE_OF_SIGHT hit=" + raycast(body, approach);
        }
        return "OK";
    }

    private boolean lineOfSight(NpcNavigationBody body, NpcInteractionRequest request, NpcInteractionApproach approach) {
        HitResult hit = raycast(body, approach);
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos target = new BlockPos(request.target().position().x(), request.target().position().y(),
                request.target().position().z());
        return ((BlockHitResult) hit).getBlockPos().equals(target);
    }

    private HitResult raycast(NpcNavigationBody body, NpcInteractionApproach approach) {
        Vec3 from = new Vec3(approach.standingPosition().x(),
                approach.standingPosition().y() + body.mob().getEyeHeight(), approach.standingPosition().z());
        Vec3 to = new Vec3(approach.hit().hitPoint().x(), approach.hit().hitPoint().y(),
                approach.hit().hitPoint().z());
        return body.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, body.mob()));
    }

    private boolean supportMissing(NpcNavigationBody body, BlockPos feet) {
        return body.level().getBlockState(feet.below()).getCollisionShape(body.level(), feet.below()).isEmpty();
    }

    private boolean bodyClear(NpcNavigationBody body, NpcInteractionApproach approach) {
        AABB box = bodyBox(body, approach);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (intersects(body, pos, box)) {
                return false;
            }
        }
        return true;
    }

    private AABB bodyBox(NpcNavigationBody body, NpcInteractionApproach approach) {
        double half = Math.max(0.15D, body.mob().getBbWidth() / 2.0D);
        double x = approach.standingPosition().x();
        double y = approach.standingPosition().y();
        double z = approach.standingPosition().z();
        return new AABB(x - half, y + 1.0E-5D, z - half, x + half,
                y + body.mob().getBbHeight() - 1.0E-5D, z + half);
    }

    private boolean intersects(NpcNavigationBody body, BlockPos pos, AABB box) {
        return body.level().getBlockState(pos).getCollisionShape(body.level(), pos).toAabbs().stream()
                .map(shape -> shape.move(pos.getX(), pos.getY(), pos.getZ()))
                .anyMatch(shape -> shape.intersects(box));
    }

    private double reachDistance(NpcNavigationBody body, NpcInteractionApproach approach) {
        Vec3 from = new Vec3(approach.standingPosition().x(),
                approach.standingPosition().y() + body.mob().getEyeHeight(), approach.standingPosition().z());
        Vec3 to = new Vec3(approach.hit().hitPoint().x(), approach.hit().hitPoint().y(),
                approach.hit().hitPoint().z());
        return from.distanceTo(to);
    }
}
