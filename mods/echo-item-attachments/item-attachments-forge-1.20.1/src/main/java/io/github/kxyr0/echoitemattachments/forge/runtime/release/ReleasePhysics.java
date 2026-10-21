package io.github.kxyr0.echoitemattachments.forge.runtime.release;

import io.github.kxyr0.echoitemattachments.api.command.ReleaseItemCommand;
import io.github.kxyr0.echoitemattachments.api.model.ReleaseMode;
import io.github.kxyr0.echoitemattachments.core.motion.Vector3d;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorSample;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ReleasePhysics {
    private static final double MAX_SPEED = 2.5D;

    public Vec3 velocity(ReleaseItemCommand command, LivingEntity entity,
            AttachmentAnchorSample anchor, Optional<Vector3d> handVelocity) {
        Vec3 velocity = switch (command.mode()) {
            case HAND_MOTION -> handVelocity.map(value -> new Vec3(value.x(), value.y(), value.z()))
                    .map(value -> value.scale(command.speed()))
                    .orElseGet(() -> entity.getLookAngle().scale(command.speed()));
            case LOOK_DIRECTION -> entity.getLookAngle().scale(command.speed());
            case LOCAL_VECTOR -> local(command, anchor);
        };
        double length = velocity.length();
        return length > MAX_SPEED ? velocity.scale(MAX_SPEED / length) : velocity;
    }

    private Vec3 local(ReleaseItemCommand command, AttachmentAnchorSample anchor) {
        Vec3 forward = anchor.forward().normalize();
        Vec3 up = anchor.up().normalize();
        Vec3 right = up.cross(forward).normalize();
        Vec3 local = right.scale(command.localX()).add(up.scale(command.localY()))
                .add(forward.scale(command.localZ()));
        return local.lengthSqr() == 0.0D ? Vec3.ZERO : local.normalize().scale(command.speed());
    }
}
