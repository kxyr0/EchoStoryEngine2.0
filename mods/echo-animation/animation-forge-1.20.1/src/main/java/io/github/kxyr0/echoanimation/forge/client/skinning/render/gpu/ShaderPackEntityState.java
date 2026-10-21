package io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional reflection boundary for the entity material currently captured by Iris/Oculus. */
final class ShaderPackEntityState {
    private Object capturedState;
    private Method entityGetter;
    private boolean resolved;

    int currentEntityId() {
        resolve();
        if (capturedState == null || entityGetter == null) {
            return 0;
        }
        try {
            return (int) entityGetter.invoke(capturedState);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> type = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState");
            Field instance = type.getField("INSTANCE");
            capturedState = instance.get(null);
            entityGetter = type.getMethod("getCurrentRenderedEntity");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            capturedState = null;
            entityGetter = null;
        }
    }
}
