package io.github.kxyr0.echoanimation.forge.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Public client-side face render request against a neutral animation subject.
 */
public record AnimationFaceRenderContext(
        AnimationSubjectId subjectId,
        List<FaceRigId> rigCandidates,
        PoseStack poses,
        MultiBufferSource buffers,
        int packedLight,
        int overlay,
        float partialTick,
        ModelPart head
) {
    public AnimationFaceRenderContext {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        rigCandidates = List.copyOf(Objects.requireNonNull(rigCandidates, "rigCandidates"));
        poses = Objects.requireNonNull(poses, "poses");
        buffers = Objects.requireNonNull(buffers, "buffers");
        head = Objects.requireNonNull(head, "head");
    }
}
