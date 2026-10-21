package io.github.kxyr0.echoplayerhud.forge.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayerhud.api.layout.HudBarDefinition;
import io.github.kxyr0.echoplayerhud.api.layout.HudBounds;
import io.github.kxyr0.echoplayerhud.api.layout.HudFramebuffer;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import io.github.kxyr0.echoplayerhud.core.layout.HudLayoutSnapshot;
import io.github.kxyr0.echoplayerhud.core.layout.HudReferenceCanvasTransform;
import io.github.kxyr0.echoplayerhud.core.render.HudBarFill;
import io.github.kxyr0.echoplayerhud.core.render.HudGradientFill;
import io.github.kxyr0.echoplayerhud.forge.client.CinematicHudVisibility;
import io.github.kxyr0.echoplayerhud.forge.service.ForgePlayerHudServices;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.slf4j.Logger;

public final class PlayerHudRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int METRIC_ISSUE_LOG_INTERVAL_TICKS = 100;
    private final HudReferenceCanvasTransform transform = new HudReferenceCanvasTransform();
    private final HudBarFill fills = new HudBarFill();
    private final HudGradientFill gradients = new HudGradientFill();
    private final PlayerHudIconResolver icons = new PlayerHudIconResolver();
    private ForgePlayerHudServices services;
    private PlayerHudDebugReport debugReport;
    private int lastMetricIssueLogTick = Integer.MIN_VALUE;

    public void useServices(ForgePlayerHudServices services) {
        this.services = services;
        this.debugReport = new PlayerHudDebugReport(services);
    }

    public void useConfigPath(Path configPath) {
        icons.useBaseDir(configPath.getParent());
    }

    public void clearIconCache() {
        icons.clear();
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible(minecraft)) {
            return;
        }
        HudFramebuffer framebuffer = framebuffer(width, height);
        if (framebuffer == null) {
            return;
        }
        HudLayoutSnapshot layout = services.layout();
        List<String> metricIssues = new ArrayList<>();
        for (HudBarDefinition bar : layout.bars()) {
            if (!bar.enabled()) {
                continue;
            }
            HudBounds bounds = transform.resolve(bar, framebuffer, layout.referenceWidth(),
                    layout.referenceHeight(), layout.scaleWithResolution());
            Optional<HudMetricSnapshot> metric = services.snapshot(bar.metricId());
            if (metric.isPresent() && metric.orElseThrow().available()) {
                draw(graphics, bounds, bar, metric.orElseThrow());
            } else if (bar.drawWhenUnavailable()) {
                metricIssues.add(metricIssue(bar, metric));
                drawBackground(graphics, bounds, bar);
                drawBorder(graphics, bounds, bar);
                drawIcon(graphics, bounds, bar);
            }
        }
        logMetricIssues(metricIssues);
    }

    private boolean visible(Minecraft minecraft) {
        return services != null && minecraft.player != null && !minecraft.player.isSleeping()
                && minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL
                && !CinematicHudVisibility.hidden();
    }

    private String metricIssue(HudBarDefinition bar, Optional<HudMetricSnapshot> metric) {
        if (metric.isEmpty()) {
            return "bar=" + bar.barId() + " metric=" + bar.metricId() + " missing_provider";
        }
        HudMetricSnapshot snapshot = metric.orElseThrow();
        return "bar=" + bar.barId() + " metric=" + bar.metricId()
                + " unavailable current=" + snapshot.current()
                + " maximum=" + snapshot.maximum()
                + " revision=" + snapshot.revision().value();
    }

    private void logMetricIssues(List<String> metricIssues) {
        if (metricIssues.isEmpty()) {
            return;
        }
        int tick = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
        if (tick == lastMetricIssueLogTick || tick % METRIC_ISSUE_LOG_INTERVAL_TICKS != 0) {
            return;
        }
        lastMetricIssueLogTick = tick;
        LOGGER.warn("Echo Player HUD metric issue(s): providers={} diagnostics={} {}",
                services.providers().size(), services.diagnostics().size(), String.join(" | ", metricIssues));
    }

    public List<String> debugBounds() {
        return debugReport == null ? List.of("Echo Player HUD renderer is not ready.") : debugReport.bounds();
    }

    private void draw(GuiGraphics graphics, HudBounds bounds, HudBarDefinition bar, HudMetricSnapshot metric) {
        HudBounds fill = fills.fill(bounds, bar.fillDirection(), metric.normalized());
        drawBackground(graphics, bounds, bar);
        drawFill(graphics, fill, bar);
        drawBorder(graphics, bounds, bar);
        drawIcon(graphics, bounds, bar);
    }

    private void drawFill(GuiGraphics graphics, HudBounds fill, HudBarDefinition bar) {
        for (HudGradientFill.Segment segment : gradients.segments(fill, bar.fillDirection(), bar.gradient(),
                bar.gradientSteps())) {
            rect(graphics, segment.bounds(), segment.argb());
        }
    }

    private void drawBackground(GuiGraphics graphics, HudBounds bounds, HudBarDefinition bar) {
        if ((bar.backgroundColor() >>> 24) != 0) {
            rect(graphics, bounds, bar.backgroundColor());
        }
    }

    private void drawBorder(GuiGraphics graphics, HudBounds bounds, HudBarDefinition bar) {
        if (bar.borderWidth() <= 0 || (bar.borderColor() >>> 24) == 0) {
            return;
        }
        int width = Math.min(scaled(bar.borderWidth(), bounds, bar),
                Math.max(1, Math.min(bounds.width(), bounds.height()) / 2));
        rect(graphics, new HudBounds(bounds.x(), bounds.y(), bounds.width(), width), bar.borderColor());
        rect(graphics, new HudBounds(bounds.x(), bounds.y() + bounds.height() - width, bounds.width(), width),
                bar.borderColor());
        rect(graphics, new HudBounds(bounds.x(), bounds.y(), width, bounds.height()), bar.borderColor());
        rect(graphics, new HudBounds(bounds.x() + bounds.width() - width, bounds.y(), width, bounds.height()),
                bar.borderColor());
    }

    private void drawIcon(GuiGraphics graphics, HudBounds bounds, HudBarDefinition bar) {
        if (bar.icon().isBlank()) {
            return;
        }
        icons.resolve(bar.icon(), bar.iconWidth(), bar.iconHeight()).ifPresent(icon -> {
            int iconWidth = scaled(bar.iconWidth(), bounds, bar);
            int iconHeight = scaled(bar.iconHeight(), bounds, bar);
            int iconSpacing = scaledSigned(bar.iconSpacing(), bounds, bar);
            double x = bounds.x() - iconSpacing - iconWidth + scaledSigned(bar.iconOffsetX(), bounds, bar);
            double y = bounds.y() + (bounds.height() - iconHeight) / 2.0D
                    + scaledSigned(bar.iconOffsetY(), bounds, bar);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0D);
            graphics.blit(icon.texture(), 0, 0, Math.max(1, iconWidth), Math.max(1, iconHeight),
                    0.0F, 0.0F, icon.width(), icon.height(), icon.width(), icon.height());
            graphics.pose().popPose();
            RenderSystem.disableBlend();
        });
    }

    private int scaled(int authoredPixels, HudBounds bounds, HudBarDefinition bar) {
        if (authoredPixels <= 0) {
            return 0;
        }
        double scale = Math.min(bounds.width() / (double) bar.width(), bounds.height() / (double) bar.height());
        return Math.max(1, (int) Math.round(authoredPixels * scale));
    }

    private int scaledSigned(int authoredPixels, HudBounds bounds, HudBarDefinition bar) {
        return authoredPixels < 0 ? -scaled(-authoredPixels, bounds, bar) : scaled(authoredPixels, bounds, bar);
    }

    private double scaledSigned(double authoredPixels, HudBounds bounds, HudBarDefinition bar) {
        double scale = Math.min(bounds.width() / (double) bar.width(), bounds.height() / (double) bar.height());
        return authoredPixels * scale;
    }

    private HudFramebuffer framebuffer(int width, int height) {
        return width <= 0 || height <= 0 ? null : new HudFramebuffer(width, height, 1.0D);
    }

    private void rect(GuiGraphics graphics, HudBounds bounds, int argb) {
        if (bounds.width() > 0 && bounds.height() > 0) {
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), argb);
        }
    }
}
