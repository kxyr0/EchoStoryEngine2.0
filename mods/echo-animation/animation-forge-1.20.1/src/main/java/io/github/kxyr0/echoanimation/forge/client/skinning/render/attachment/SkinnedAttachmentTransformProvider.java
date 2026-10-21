package io.github.kxyr0.echoanimation.forge.client.skinning.render.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationAttachmentTransformSpace;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationBoneTransformProvider;
import io.github.kxyr0.echoanimation.forge.api.client.render.attachment.AnimationHumanoidAttachmentAnchor;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import io.github.kxyr0.echoanimation.forge.client.skinning.pose.SkinningPalette;
import java.util.Objects;
import org.joml.Matrix4f;

/** Applies rig-owned attachment socket bindings in the same model space as skinned vertices. */
public final class SkinnedAttachmentTransformProvider
        implements AnimationBoneTransformProvider {

    private static final float PIXEL = 1.0F / 16.0F;

    private final SkinningPalette palette;
    private final SkinnedModel model;
    private final AnimationPose pose;
    private final boolean freezeBindPose;

    public SkinnedAttachmentTransformProvider(
            SkinningPalette palette,
            SkinnedModel model,
            AnimationPose pose,
            boolean freezeBindPose) {

        this.palette = Objects.requireNonNull(
                palette,
                "palette");
        this.model = Objects.requireNonNull(
                model,
                "model");
        this.pose = Objects.requireNonNull(
                pose,
                "pose");
        this.freezeBindPose = freezeBindPose;
    }

    @Override
    public void apply(
            String boneName,
            PoseStack poses) {

        applyMatrix(
                palette.global(
                        model,
                        pose,
                        boneName,
                        freezeBindPose),
                poses);
    }

    @Override
    public void applyAnchor(
            AnimationHumanoidAttachmentAnchor anchor,
            PoseStack poses) {

        if (anchorSpace(anchor)
                == AnimationAttachmentTransformSpace.MODEL_ABSOLUTE) {
            poses.translate(
                    model.renderOffsetX() * PIXEL,
                    model.renderOffsetY() * PIXEL,
                    model.renderOffsetZ() * PIXEL);
        }

        String boneName =
                model.attachmentBindings()
                        .bone(anchor);
        Matrix4f socket =
                palette.socketGlobal(
                        model,
                        pose,
                        boneName,
                        freezeBindPose,
                        model.attachmentBindings()
                                .cancelBindBasis());

        applyMatrix(
                socket,
                poses);
    }

    @Override
    public AnimationAttachmentTransformSpace anchorSpace(
            AnimationHumanoidAttachmentAnchor anchor) {

        return model.attachmentBindings()
                .space();
    }

    private void applyMatrix(
            Matrix4f matrix,
            PoseStack poses) {

        poses.mulPoseMatrix(
                new Matrix4f()
                        .scale(PIXEL)
                        .mul(matrix)
                        .scale(1.0F / PIXEL));
    }
}
