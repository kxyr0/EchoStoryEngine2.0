package io.github.kxyr0.echonpc.forge.api.client.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Client-only view of generic NPC body anchors during a render pass.
 */
public record NpcBodyAttachmentRenderContext(
        ActorId actorId,
        PoseStack poses,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay,
        float partialTick,
        ModelPart torso,
        ModelPart leftArm,
        ModelPart rightArm,
        boolean visible,
        String definitionId,
        String formId,
        boolean child,
        boolean sitting,
        boolean sleeping,
        float bodyScale,
        NpcVisualAttachmentTransformProvider anchorTransforms
) {

    public NpcBodyAttachmentRenderContext {
        actorId = Objects.requireNonNull(
                actorId,
                "actorId");

        poses = Objects.requireNonNull(
                poses,
                "poses");

        buffers = Objects.requireNonNull(
                buffers,
                "buffers");

        torso = Objects.requireNonNull(
                torso,
                "torso");

        leftArm = Objects.requireNonNull(
                leftArm,
                "leftArm");

        rightArm = Objects.requireNonNull(
                rightArm,
                "rightArm");

        definitionId = Objects.requireNonNull(
                definitionId,
                "definitionId");

        formId = Objects.requireNonNull(
                formId,
                "formId");

        anchorTransforms = Objects.requireNonNull(
                anchorTransforms,
                "anchorTransforms");

        if (!Float.isFinite(bodyScale)
                || bodyScale <= 0.0F) {
            throw new IllegalArgumentException(
                    "bodyScale must be finite and positive.");
        }
    }

    /**
     * Compatibility constructor for normal vanilla attachment rendering.
     */
    public NpcBodyAttachmentRenderContext(
            ActorId actorId,
            PoseStack poses,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            float partialTick,
            ModelPart torso,
            ModelPart leftArm,
            ModelPart rightArm,
            boolean visible,
            String definitionId,
            String formId,
            boolean child,
            boolean sitting,
            boolean sleeping,
            float bodyScale) {

        this(
                actorId,
                poses,
                buffers,
                packedLight,
                packedOverlay,
                partialTick,
                torso,
                leftArm,
                rightArm,
                visible,
                definitionId,
                formId,
                child,
                sitting,
                sleeping,
                bodyScale,
                vanillaAnchors(
                        torso,
                        leftArm,
                        rightArm));
    }

    public ModelPart anchor(
            NpcVisualAttachmentAnchor anchor) {

        return switch (
                Objects.requireNonNull(
                        anchor,
                        "anchor")) {

            case TORSO_BACK -> torso;
            case LEFT_HAND -> leftArm;
            case RIGHT_HAND -> rightArm;
        };
    }

    public void applyAnchor(
            NpcVisualAttachmentAnchor anchor) {

        anchorTransforms.apply(
                Objects.requireNonNull(
                        anchor,
                        "anchor"),
                poses);
    }

    private static NpcVisualAttachmentTransformProvider vanillaAnchors(
            ModelPart torso,
            ModelPart leftArm,
            ModelPart rightArm) {

        return (anchor, poses) -> {
            ModelPart part = switch (anchor) {
                case TORSO_BACK -> torso;
                case LEFT_HAND -> leftArm;
                case RIGHT_HAND -> rightArm;
            };

            part.translateAndRotate(poses);
        };
    }
}
