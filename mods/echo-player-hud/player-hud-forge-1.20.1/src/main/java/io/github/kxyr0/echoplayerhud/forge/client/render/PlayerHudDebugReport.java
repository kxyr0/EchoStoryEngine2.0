package io.github.kxyr0.echoplayerhud.forge.client.render;

import io.github.kxyr0.echoplayerhud.api.layout.HudBarDefinition;
import io.github.kxyr0.echoplayerhud.api.layout.HudBounds;
import io.github.kxyr0.echoplayerhud.api.layout.HudFramebuffer;
import io.github.kxyr0.echoplayerhud.core.layout.HudLayoutSnapshot;
import io.github.kxyr0.echoplayerhud.core.layout.HudReferenceCanvasTransform;
import io.github.kxyr0.echoplayerhud.forge.service.ForgePlayerHudServices;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

final class PlayerHudDebugReport {
    private final ForgePlayerHudServices services;
    private final HudReferenceCanvasTransform transform = new HudReferenceCanvasTransform();

    PlayerHudDebugReport(ForgePlayerHudServices services) {
        this.services = services;
    }

    List<String> bounds() {
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (width <= 0 || height <= 0) {
            return List.of("Echo Player HUD framebuffer is not ready.");
        }
        HudFramebuffer framebuffer = new HudFramebuffer(width, height, 1.0D);
        HudLayoutSnapshot layout = services.layout();
        List<String> lines = new ArrayList<>();
        for (HudBarDefinition bar : layout.bars()) {
            HudBounds result = transform.resolve(bar, framebuffer, layout.referenceWidth(),
                    layout.referenceHeight(), layout.scaleWithResolution());
            lines.add(describe(bar, result, layout, framebuffer));
        }
        return List.copyOf(lines);
    }

    private static String describe(
            HudBarDefinition bar,
            HudBounds bounds,
            HudLayoutSnapshot layout,
            HudFramebuffer framebuffer
    ) {
        return bar.barId() + " metric=" + bar.metricId() + " anchor=" + bar.anchor()
                + " offset=" + bar.offsetX() + "," + bar.offsetY()
                + " size=" + bar.width() + "x" + bar.height()
                + " enabled=" + bar.enabled() + " border=" + bar.borderWidth()
                + " icon=" + (bar.icon().isBlank() ? "none" : bar.icon())
                + " iconOffset=" + bar.iconOffsetX() + "," + bar.iconOffsetY()
                + " gradientSteps=" + bar.gradientSteps()
                + " framebuffer=" + bounds.x() + "," + bounds.y() + ","
                + bounds.width() + "," + bounds.height()
                + " reference=" + layout.referenceWidth() + "x" + layout.referenceHeight()
                + " scaleWithResolution=" + layout.scaleWithResolution()
                + " guiScale=" + framebuffer.guiScale();
    }
}
