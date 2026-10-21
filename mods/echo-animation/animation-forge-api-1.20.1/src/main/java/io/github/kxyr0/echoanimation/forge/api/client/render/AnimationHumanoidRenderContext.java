package io.github.kxyr0.echoanimation.forge.api.client.render;

import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationHumanoidAttachmentRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.api.id.FaceRigId;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/**
 * Public client-side full humanoid render request against a neutral subject.
 */
public record AnimationHumanoidRenderContext(
        AnimationSubjectId subjectId,
        List<AnimationRigId> rigCandidates,
        List<FaceRigId> faceRigCandidates,
        ResourceLocation skinTexture,
        AnimationSkinVariant skinVariant,
        PoseStack poses,
        MultiBufferSource buffers,
        int packedLight,
        int overlay,
        float partialTick,
        boolean sitting,
        AnimationHumanoidAttachmentRenderer attachments
) {

    public AnimationHumanoidRenderContext {
        subjectId =
                Objects.requireNonNull(
                        subjectId,
                        "subjectId");

        rigCandidates =
                List.copyOf(
                        Objects.requireNonNull(
                                rigCandidates,
                                "rigCandidates"));

        faceRigCandidates =
                List.copyOf(
                        Objects.requireNonNull(
                                faceRigCandidates,
                                "faceRigCandidates"));

        skinTexture =
                Objects.requireNonNull(
                        skinTexture,
                        "skinTexture");

        skinVariant =
                Objects.requireNonNull(
                        skinVariant,
                        "skinVariant");

        poses =
                Objects.requireNonNull(
                        poses,
                        "poses");

        buffers =
                Objects.requireNonNull(
                        buffers,
                        "buffers");

        attachments =
                Objects.requireNonNull(
                        attachments,
                        "attachments");
    }

}
