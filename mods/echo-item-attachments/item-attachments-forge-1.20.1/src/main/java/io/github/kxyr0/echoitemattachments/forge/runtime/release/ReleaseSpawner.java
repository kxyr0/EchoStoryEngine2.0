package io.github.kxyr0.echoitemattachments.forge.runtime.release;

import io.github.kxyr0.echoitemattachments.api.command.ReleaseItemCommand;
import io.github.kxyr0.echoitemattachments.api.identity.ReleaseId;
import io.github.kxyr0.echoitemattachments.core.motion.Vector3d;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorSample;
import io.github.kxyr0.echoitemattachments.core.model.ReleasePlan;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class ReleaseSpawner {
    private final ReleaseWitness witnesses = new ReleaseWitness();
    private final ReleasePhysics physics = new ReleasePhysics();

    public ItemEntity spawn(ServerLevel level, ItemStack stack, ReleaseId releaseId,
            ReleaseItemCommand command, LivingEntity entity, AttachmentAnchorSample anchor,
            Optional<Vector3d> handVelocity) {
        ItemEntity released = new ItemEntity(level, anchor.position().x, anchor.position().y,
                anchor.position().z, stack.copy());
        released.setDeltaMovement(physics.velocity(command, entity, anchor, handVelocity));
        released.setPickUpDelay(20);
        witnesses.mark(released, releaseId);
        if (!level.addFreshEntity(released)) {
            throw new IllegalStateException("EIA-RELEASE-SPAWN-FAILED: world rejected ItemEntity");
        }
        return released;
    }

    public ItemEntity spawnPrepared(ServerLevel level, ItemStack stack,
            ReleaseId releaseId, ReleasePlan plan) {
        ItemEntity released = new ItemEntity(level, plan.x(), plan.y(), plan.z(), stack.copy());
        released.setDeltaMovement(plan.velocityX(), plan.velocityY(), plan.velocityZ());
        released.setPickUpDelay(20);
        witnesses.mark(released, releaseId);
        if (!level.addFreshEntity(released)) {
            throw new IllegalStateException("EIA-RELEASE-SPAWN-FAILED: world rejected ItemEntity");
        }
        return released;
    }
}
