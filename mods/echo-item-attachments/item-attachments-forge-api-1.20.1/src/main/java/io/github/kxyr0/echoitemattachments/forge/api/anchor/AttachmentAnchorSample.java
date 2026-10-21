package io.github.kxyr0.echoitemattachments.forge.api.anchor;

import net.minecraft.world.phys.Vec3;

public record AttachmentAnchorSample(
        Vec3 position,
        Vec3 forward,
        Vec3 up,
        long serverTick,
        long poseRevision) {
    public AttachmentAnchorSample {
        if (position == null || forward == null || up == null) {
            throw new IllegalArgumentException("anchor vectors are required");
        }
        if (serverTick < 0 || poseRevision < 0) {
            throw new IllegalArgumentException("tick and revision must be non-negative");
        }
    }
}
