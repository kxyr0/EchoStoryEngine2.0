package io.github.kxyr0.echoanimation.forge.api.client.render.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Render frame exposed to client-side pose overlay listeners.
 */
public record AnimationHumanoidPoseRenderFrame(
        PoseStack poses,
        MultiBufferSource buffers,
        int packedLight,
        float partialTick
) {
    public AnimationHumanoidPoseRenderFrame {
        poses = Objects.requireNonNull(poses, "poses");
        buffers = Objects.requireNonNull(buffers, "buffers");
    }
}
