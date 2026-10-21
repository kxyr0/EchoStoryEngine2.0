package io.github.kxyr0.echoanimation.forge.api.client.render;

/**
 * Public client-only Echo Animation presentation service.
 */
public interface AnimationClientService {
    AnimationFaceRenderResult renderFace(AnimationFaceRenderContext context);

    default AnimationHumanoidRenderResult renderHumanoid(AnimationHumanoidRenderContext context) {
        return AnimationHumanoidRenderResult.NOT_HANDLED;
    }
}
