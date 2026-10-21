package io.github.kxyr0.echonpc.forge.client.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentAnchor;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentTransformProvider;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentTransformSpace;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import java.util.Objects;
import net.minecraft.client.model.PlayerModel;

/** Composes animation-owned anchor transforms with legacy vanilla pivots only when required. */
public final class NpcReplacementAttachmentAnchorProvider
        implements NpcVisualAttachmentTransformProvider {

    private final PlayerModel<EchoNpcEntity> model;
    private final NpcVisualAttachmentTransformProvider delegate;

    public NpcReplacementAttachmentAnchorProvider(
            PlayerModel<EchoNpcEntity> model,
            NpcVisualAttachmentTransformProvider delegate) {

        this.model = Objects.requireNonNull(
                model,
                "model");
        this.delegate = Objects.requireNonNull(
                delegate,
                "delegate");
    }

    @Override
    public void apply(
            NpcVisualAttachmentAnchor anchor,
            PoseStack poses) {

        delegate.apply(
                anchor,
                poses);

        if (delegate.space(anchor)
                == NpcVisualAttachmentTransformSpace.PART_RELATIVE) {
            applyVanillaPivot(
                    anchor,
                    poses);
        }
    }

    @Override
    public NpcVisualAttachmentTransformSpace space(
            NpcVisualAttachmentAnchor anchor) {

        return delegate.space(anchor);
    }

    private void applyVanillaPivot(
            NpcVisualAttachmentAnchor anchor,
            PoseStack poses) {

        switch (anchor) {
            case TORSO_BACK ->
                    model.body
                            .translateAndRotate(poses);

            case LEFT_HAND ->
                    model.leftArm
                            .translateAndRotate(poses);

            case RIGHT_HAND ->
                    model.rightArm
                            .translateAndRotate(poses);
        }
    }
}
