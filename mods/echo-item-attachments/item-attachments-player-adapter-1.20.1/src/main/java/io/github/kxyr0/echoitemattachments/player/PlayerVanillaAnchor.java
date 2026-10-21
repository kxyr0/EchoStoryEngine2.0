package io.github.kxyr0.echoitemattachments.player;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.forge.api.anchor.AttachmentAnchorSample;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class PlayerVanillaAnchor {
    private PlayerVanillaAnchor() {
    }

    static AttachmentAnchorSample sample(
            LivingEntity entity, AttachmentPointId point, long serverTick) {
        float yaw = entity.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        double side = point.equals(AttachmentPointId.RIGHT_HAND) ? 0.36D : -0.36D;
        Vec3 position = entity.position().add(0.0D, entity.getBbHeight() * 0.61D, 0.0D)
                .add(right.scale(side)).add(forward.scale(0.10D));
        return new AttachmentAnchorSample(position, forward, new Vec3(0.0D, 1.0D, 0.0D),
                serverTick, serverTick);
    }
}
