package io.github.kxyr0.echoanimation.forge.client.skinning.render;

import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import org.joml.Matrix4f;

/** Exchangeable GPU production path and CPU reference fallback. */
interface SkinningBackend {
    void render(SkinnedModel model, Matrix4f[] palette, AnimationHumanoidRenderContext context, int debugMode);

    default void clear() {
    }
}
