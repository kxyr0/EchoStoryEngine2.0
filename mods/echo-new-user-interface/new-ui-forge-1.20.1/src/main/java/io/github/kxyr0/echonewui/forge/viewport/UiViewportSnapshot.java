package io.github.kxyr0.echonewui.forge.viewport;

import com.mojang.blaze3d.platform.Window;
import io.github.kxyr0.echonewui.core.layout.UiViewport;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnosticCode;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnostics;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;

public final class UiViewportSnapshot {
    private static final AtomicLong REVISION = new AtomicLong(1L);
    private static volatile Metrics lastMetrics = new Metrics(0, 0, 0, 0, 0.0D);

    private UiViewportSnapshot() {
    }

    public static Optional<UiViewport> capture(NewUiDiagnostics diagnostics) {
        Window window = Minecraft.getInstance().getWindow();
        try {
            Metrics metrics = new Metrics(window.getWidth(), window.getHeight(),
                    window.getGuiScaledWidth(), window.getGuiScaledHeight(), window.getGuiScale());
            return Optional.of(new UiViewport(metrics.framebufferWidth(), metrics.framebufferHeight(),
                    metrics.guiWidth(), metrics.guiHeight(), metrics.guiScale(), revision(metrics)));
        } catch (RuntimeException exception) {
            diagnostics.warn(NewUiDiagnosticCode.NEW_UI_VIEWPORT_INVALID, exception.getMessage());
            return Optional.empty();
        }
    }

    private static long revision(Metrics metrics) {
        Metrics previous = lastMetrics;
        if (metrics.equals(previous)) {
            return REVISION.get();
        }
        lastMetrics = metrics;
        return REVISION.incrementAndGet();
    }

    private record Metrics(
            int framebufferWidth,
            int framebufferHeight,
            int guiWidth,
            int guiHeight,
            double guiScale) {
    }
}
