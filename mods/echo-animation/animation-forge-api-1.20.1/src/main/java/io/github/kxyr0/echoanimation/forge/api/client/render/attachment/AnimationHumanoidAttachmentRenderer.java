package io.github.kxyr0.echoanimation.forge.api.client.render.attachment;

@FunctionalInterface
public interface AnimationHumanoidAttachmentRenderer {

    AnimationHumanoidAttachmentRenderer NONE =
            bones -> {
            };

    void render(
            AnimationBoneTransformProvider bones);
}
