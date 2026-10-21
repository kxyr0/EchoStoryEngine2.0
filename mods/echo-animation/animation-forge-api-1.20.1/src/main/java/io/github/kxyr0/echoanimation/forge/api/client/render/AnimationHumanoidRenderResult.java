package io.github.kxyr0.echoanimation.forge.api.client.render;

/**
 * Explicit result of a public segmented humanoid render request.
 */
public enum AnimationHumanoidRenderResult {
    NOT_HANDLED,
    RENDERED_OVERLAY,
    REPLACED_DEFAULT_MODEL,
    FAILED
}
