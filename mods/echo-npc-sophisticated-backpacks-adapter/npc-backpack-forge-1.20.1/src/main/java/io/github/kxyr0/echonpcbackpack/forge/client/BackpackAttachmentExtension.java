package io.github.kxyr0.echonpcbackpack.forge.client;

import com.mojang.math.Axis;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderContext;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcBodyAttachmentRenderExtension;
import io.github.kxyr0.echonpc.forge.api.client.attachment.NpcVisualAttachmentAnchor;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderResult;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackAttachmentPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Renders the exact synchronized stack through Sophisticated Backpacks'
 * item renderer.
 */
public final class BackpackAttachmentExtension
    implements NpcBodyAttachmentRenderExtension {

  private final BackpackTransformCatalog transforms;

  public BackpackAttachmentExtension(BackpackTransformCatalog transforms) {
    this.transforms = transforms;
  }

  @Override
  public NpcClientRenderResult render(NpcBodyAttachmentRenderContext context) {

    boolean debug = false;

    if (debug) {
      renderDebugAnchors(context);
    }

    var snapshot = BackpackClientState.get(context.actorId());

    if (!context.visible() || snapshot.isEmpty() ||
        snapshot.orElseThrow().payload().isEmpty() ||
        snapshot.orElseThrow().attachmentPoint() ==
            NpcBackpackAttachmentPoint.NONE) {

      return debug ? NpcClientRenderResult.RENDERED_OVERLAY
                   : NpcClientRenderResult.NOT_HANDLED;
    }

    var value = snapshot.orElseThrow();

    BackpackAttachmentTransform transform =
        transforms.resolve(context.formId(), value.attachmentPoint());

    context.poses().pushPose();

    try {
      context.applyAnchor(anchor(value.attachmentPoint()));

      context.poses().translate(transform.x(), transform.y(),
                                transform.z() - 0.10F);

      context.poses().mulPose(Axis.XP.rotationDegrees(transform.rotateX()));

      context.poses().mulPose(Axis.YP.rotationDegrees(transform.rotateY()));

      context.poses().mulPose(Axis.ZP.rotationDegrees(transform.rotateZ()));

      float testScale = transform.scale() * 1.25F;

      context.poses().scale(testScale, testScale, testScale);

      Minecraft minecraft = Minecraft.getInstance();

      minecraft.getItemRenderer().renderStatic(
          value.payload(), ItemDisplayContext.FIXED, context.packedLight(),
          context.packedOverlay(), context.poses(), context.buffers(),
          minecraft.level, context.actorId().hashCode());
    } finally {
      context.poses().popPose();
    }

    return NpcClientRenderResult.RENDERED_OVERLAY;
  }

  private void renderDebugAnchors(NpcBodyAttachmentRenderContext context) {

    renderAnchorBox(context, NpcVisualAttachmentAnchor.TORSO_BACK, 1.0F, 0.2F,
                    0.2F);
  }

  private void renderAnchorBox(NpcBodyAttachmentRenderContext context,
                               NpcVisualAttachmentAnchor anchor, float red,
                               float green, float blue) {

    context.poses().pushPose();

    try {
      context.applyAnchor(anchor);

      LevelRenderer.renderLineBox(
          context.poses(), context.buffers().getBuffer(RenderType.lines()),
          -0.30D, -0.30D, -0.30D, 0.30D, 0.30D, 0.30D, red, green, blue, 1.0F);
    } finally {
      context.poses().popPose();
    }
  }

  private NpcVisualAttachmentAnchor anchor(NpcBackpackAttachmentPoint point) {

    return switch (point) {
      case BACK -> NpcVisualAttachmentAnchor.TORSO_BACK;

      case LEFT_HAND -> NpcVisualAttachmentAnchor.LEFT_HAND;

      case RIGHT_HAND -> NpcVisualAttachmentAnchor.RIGHT_HAND;

      case NONE ->
        throw new IllegalArgumentException("NONE has no render anchor.");
    };
  }
}