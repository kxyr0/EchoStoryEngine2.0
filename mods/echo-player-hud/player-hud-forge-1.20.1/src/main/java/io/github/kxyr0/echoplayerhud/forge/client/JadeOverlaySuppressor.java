package io.github.kxyr0.echoplayerhud.forge.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraftforge.client.event.RenderGuiEvent;

public final class JadeOverlaySuppressor {
    private boolean initialized;
    private boolean unavailable;
    private Method wailaInstance;
    private Field tooltipRenderer;
    private Field overlayAlpha;
    private Field overlayShown;

    public JadeOverlaySuppressor() {
    }

    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!CinematicHudVisibility.hidden()) {
            return;
        }
        suppress();
    }

    private void suppress() {
        if (unavailable) {
            return;
        }
        try {
            ensureInitialized();
            Object handler = wailaInstance.invoke(null);
            if (handler != null) {
                tooltipRenderer.set(handler, null);
            }
            overlayAlpha.setFloat(null, 0.0F);
            overlayShown.setBoolean(null, false);
        } catch (ReflectiveOperationException | LinkageError exception) {
            unavailable = true;
        }
    }

    private void ensureInitialized() throws ReflectiveOperationException {
        if (initialized) {
            return;
        }
        Class<?> overlayType = Class.forName("snownee.jade.overlay.OverlayRenderer");
        overlayAlpha = overlayType.getField("alpha");
        overlayShown = overlayType.getField("shown");
        Class<?> handlerType = Class.forName("snownee.jade.overlay.WailaTickHandler");
        wailaInstance = handlerType.getMethod("instance");
        tooltipRenderer = handlerType.getField("tooltipRenderer");
        initialized = true;
    }
}
