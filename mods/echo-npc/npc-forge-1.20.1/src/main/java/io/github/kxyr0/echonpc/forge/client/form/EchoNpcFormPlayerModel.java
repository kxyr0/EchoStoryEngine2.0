package io.github.kxyr0.echonpc.forge.client.form;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyPartId;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * PlayerModel wrapper that applies cached per-form semantic part transforms.
 */
public final class EchoNpcFormPlayerModel<T extends LivingEntity> extends PlayerModel<T> {
    private final Map<NpcBodyPartId, Transform> transforms = new EnumMap<>(NpcBodyPartId.class);

    public EchoNpcFormPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    public void applyFormParts(String encodedParts) {
        transforms.clear();
        if (encodedParts == null || encodedParts.isBlank()) {
            return;
        }
        for (String entry : encodedParts.split(";")) {
            Transform.parse(entry).ifPresent(transform -> transforms.put(transform.partId(), transform));
        }
    }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        var context = new RenderContext(poses, consumer, light, overlay, red, green, blue, alpha);
        renderPart(context, NpcBodyPartId.HEAD, head);
        renderPart(context, NpcBodyPartId.HEAD, hat);
        renderPart(context, NpcBodyPartId.BODY, body);
        renderPart(context, NpcBodyPartId.RIGHT_ARM, rightArm);
        renderPart(context, NpcBodyPartId.LEFT_ARM, leftArm);
        renderPart(context, NpcBodyPartId.RIGHT_LEG, rightLeg);
        renderPart(context, NpcBodyPartId.LEFT_LEG, leftLeg);
        renderPart(context, NpcBodyPartId.BODY, jacket);
        renderPart(context, NpcBodyPartId.LEFT_ARM, leftSleeve);
        renderPart(context, NpcBodyPartId.RIGHT_ARM, rightSleeve);
        renderPart(context, NpcBodyPartId.LEFT_LEG, leftPants);
        renderPart(context, NpcBodyPartId.RIGHT_LEG, rightPants);
    }

    private void renderPart(RenderContext context, NpcBodyPartId id, ModelPart part) {
        Transform transform = transforms.get(id);
        if (transform == null) {
            context.render(part);
            return;
        }
        PoseStack poses = context.poses();
        poses.pushPose();
        poses.translate(transform.pivotX(), transform.pivotY(), transform.pivotZ());
        poses.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) transform.rotationX()));
        poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) transform.rotationY()));
        poses.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) transform.rotationZ()));
        poses.scale((float) transform.scaleX(), (float) transform.scaleY(), (float) transform.scaleZ());
        context.render(part);
        poses.popPose();
    }

    private record RenderContext(PoseStack poses, VertexConsumer consumer, int light, int overlay,
            float red, float green, float blue, float alpha) {
        void render(ModelPart part) {
            part.render(poses, consumer, light, overlay, red, green, blue, alpha);
        }
    }
}
