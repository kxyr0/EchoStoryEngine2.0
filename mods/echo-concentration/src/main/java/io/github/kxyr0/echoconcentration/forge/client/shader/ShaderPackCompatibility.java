package io.github.kxyr0.echoconcentration.forge.client.shader;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

/** Optional Oculus/Iris API probe; no shader mod is required to load concentration. */
public final class ShaderPackCompatibility {
    private static final BooleanSupplier ACTIVE = discover();

    private ShaderPackCompatibility() {
    }

    public static boolean active() {
        return ACTIVE.getAsBoolean();
    }

    private static BooleanSupplier discover() {
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method inUse = apiClass.getMethod("isShaderPackInUse");
            return () -> query(api, inUse);
        } catch (ClassNotFoundException exception) {
            return () -> false;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return () -> true;
        }
    }

    private static boolean query(Object api, Method inUse) {
        try {
            return Boolean.TRUE.equals(inUse.invoke(api));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return true;
        }
    }
}
