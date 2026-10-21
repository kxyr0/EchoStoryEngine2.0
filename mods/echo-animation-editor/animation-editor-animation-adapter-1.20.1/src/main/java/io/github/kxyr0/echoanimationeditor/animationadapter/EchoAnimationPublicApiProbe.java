package io.github.kxyr0.echoanimationeditor.animationadapter;

import io.github.kxyr0.echoanimationeditor.api.diagnostic.AnimatorPublicApiStatus;
import java.lang.reflect.InvocationTargetException;

public final class EchoAnimationPublicApiProbe {
    private static final String BOUNDARY = "echo_animation_client";
    private static final String BRIDGE =
            "io.github.kxyr0.echoanimation.forge.api.client.render.AnimationClientServiceBridge";

    public AnimatorPublicApiStatus probeClient() {
        try {
            Class<?> bridge = Class.forName(BRIDGE);
            Object result = bridge.getMethod("lookup").invoke(null);
            return new AnimatorPublicApiStatus(BOUNDARY, value(result, "status"), value(result, "message"));
        } catch (ClassNotFoundException exception) {
            return AnimatorPublicApiStatus.unavailable(BOUNDARY, "Echo Animation public client bridge not found.");
        } catch (IllegalAccessException | NoSuchMethodException exception) {
            return AnimatorPublicApiStatus.unavailable(BOUNDARY, "Echo Animation public bridge signature mismatch.");
        } catch (InvocationTargetException exception) {
            return AnimatorPublicApiStatus.unavailable(BOUNDARY, exception.getTargetException().getMessage());
        }
    }

    private static String value(Object target, String method)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object value = target.getClass().getMethod(method).invoke(target);
        return String.valueOf(value);
    }
}
