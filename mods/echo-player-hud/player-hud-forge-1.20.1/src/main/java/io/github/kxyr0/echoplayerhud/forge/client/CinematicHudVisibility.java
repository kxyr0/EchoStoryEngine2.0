package io.github.kxyr0.echoplayerhud.forge.client;

import java.lang.reflect.Method;

public final class CinematicHudVisibility {
    private CinematicHudVisibility() {
    }

    public static boolean hidden() {
        return staticBoolean("io.github.kxyr0.echodialogue.forge.client.ClientDialogueClient", "hudSuppressed")
                || staticBoolean("io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient", "hudSuppressed");
    }

    private static boolean staticBoolean(String className, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod(methodName);
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }
}
