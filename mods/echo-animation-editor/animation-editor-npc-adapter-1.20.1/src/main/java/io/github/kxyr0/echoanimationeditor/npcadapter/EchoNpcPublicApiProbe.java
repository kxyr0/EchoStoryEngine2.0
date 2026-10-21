package io.github.kxyr0.echoanimationeditor.npcadapter;

import io.github.kxyr0.echoanimationeditor.api.diagnostic.AnimatorPublicApiStatus;
import java.lang.reflect.InvocationTargetException;

public final class EchoNpcPublicApiProbe {
    private static final String BOUNDARY = "echo_npc_service";
    private static final String BRIDGE = "io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge";
    private static final String SERVER = "net.minecraft.server.MinecraftServer";

    public AnimatorPublicApiStatus probe(Object server) {
        if (server == null) {
            return AnimatorPublicApiStatus.unavailable(BOUNDARY, "MinecraftServer is not available on this client.");
        }
        try {
            Class<?> bridge = Class.forName(BRIDGE);
            Class<?> serverType = Class.forName(SERVER);
            Object result = bridge.getMethod("lookup", serverType).invoke(null, server);
            return new AnimatorPublicApiStatus(BOUNDARY, value(result, "status"), value(result, "message"));
        } catch (ClassNotFoundException exception) {
            return AnimatorPublicApiStatus.unavailable(BOUNDARY, "Echo NPC public service bridge not found.");
        } catch (IllegalAccessException | NoSuchMethodException exception) {
            return AnimatorPublicApiStatus.unavailable(BOUNDARY, "Echo NPC public bridge signature mismatch.");
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
