package io.github.kxyr0.echotimehud.forge.client;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotimehud.api.HudConfig;
import io.github.kxyr0.echotimehud.api.HudScreenPolicy;
import io.github.kxyr0.echotimehud.format.TimeHudFormatter;
import io.github.kxyr0.echotimehud.forge.config.HudConfigLoader;
import io.github.kxyr0.echotimehud.geometry.HudLayoutInput;
import io.github.kxyr0.echotimehud.geometry.HudLayoutResult;
import io.github.kxyr0.echotimehud.geometry.PhysicalRect;
import io.github.kxyr0.echotimehud.geometry.TimeHudLayout;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class TimeHudRenderer {
    private final TimeHudSnapshotStore snapshots;
    private final HudConfigLoader configs;
    private final TimeHudLayout layout = new TimeHudLayout();
    private final TimeHudFormatter formatter = new TimeHudFormatter();
    private volatile HudLayoutResult lastLayout;
    private volatile String lastText = "";
    private volatile boolean debugBounds;

    public TimeHudRenderer(TimeHudSnapshotStore snapshots, HudConfigLoader configs) {
        this.snapshots = snapshots;
        this.configs = configs;
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int ignoredWidth, int ignoredHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        HudConfig config = configs.current();
        Optional<EchoTimeSnapshot> snapshot = snapshots.current();
        if (!visible(minecraft, config) || snapshot.isEmpty()) return;
        EchoTimeSnapshot value = snapshot.orElseThrow();
        String text = formatter.format(value.instant().day().value(), value.instant().timeOfDay().hour(),
                value.instant().timeOfDay().minute(), config);
        Font font = minecraft.font;
        int framebufferWidth = minecraft.getWindow().getWidth();
        int framebufferHeight = minecraft.getWindow().getHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) return;
        HudLayoutResult placement = layout.place(new HudLayoutInput(framebufferWidth, framebufferHeight,
                font.width(text), font.lineHeight, config));
        lastLayout = placement;
        lastText = text;
        draw(graphics, font, text, config, placement, minecraft);
        if (debugBounds) drawDebug(graphics, placement, minecraft);
    }

    private boolean visible(Minecraft minecraft, HudConfig config) {
        if (!config.enabled() || minecraft.level == null || minecraft.player == null) return false;
        if (config.respectHideHud() && minecraft.options.hideGui) return false;
        return config.screenPolicy() == HudScreenPolicy.SHOW_DURING_GAME_SCREENS || minecraft.screen == null;
    }

    private void draw(GuiGraphics graphics, Font font, String text, HudConfig config,
                      HudLayoutResult placement, Minecraft minecraft) {
        double pxPerGuiX = (double) minecraft.getWindow().getWidth() / minecraft.getWindow().getGuiScaledWidth();
        double pxPerGuiY = (double) minecraft.getWindow().getHeight() / minecraft.getWindow().getGuiScaledHeight();
        double scaleX = config.textScale() * placement.uniformScale() / pxPerGuiX;
        double scaleY = config.textScale() * placement.uniformScale() / pxPerGuiY;
        graphics.pose().pushPose();
        graphics.pose().translate(placement.finalTextRect().x() / pxPerGuiX,
                placement.finalTextRect().y() / pxPerGuiY, 0.0);
        graphics.pose().scale((float) scaleX, (float) scaleY, 1.0F);
        int alpha = (int) Math.round(config.opacity() * 255.0);
        if (config.background()) {
            int backgroundAlpha = (int) Math.round(config.opacity() * 0.45 * 255.0);
            graphics.fill(-2, -2, font.width(text) + 2, font.lineHeight + 2, backgroundAlpha << 24);
        }
        graphics.drawString(font, text, 0, 0, (alpha << 24) | 0xFFFFFF, config.shadow());
        graphics.pose().popPose();
    }

    private void drawDebug(GuiGraphics graphics, HudLayoutResult placement, Minecraft minecraft) {
        double sx = (double) minecraft.getWindow().getWidth() / minecraft.getWindow().getGuiScaledWidth();
        double sy = (double) minecraft.getWindow().getHeight() / minecraft.getWindow().getGuiScaledHeight();
        outline(graphics, placement.referenceCanvas(), sx, sy, 0xA00080FF);
        outline(graphics, placement.safeRect(), sx, sy, 0xA000FF80);
        outline(graphics, placement.finalTextRect(), sx, sy, 0xA0FF4040);
    }

    private void outline(GuiGraphics graphics, PhysicalRect rect, double sx, double sy, int color) {
        int left = (int) Math.floor(rect.x() / sx), right = (int) Math.ceil(rect.right() / sx);
        int top = (int) Math.floor(rect.y() / sy), bottom = (int) Math.ceil(rect.bottom() / sy);
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    public void debugBounds(boolean enabled) { debugBounds = enabled; }
    public boolean debugBounds() { return debugBounds; }

    public List<String> debugReport() {
        HudLayoutResult value = lastLayout;
        if (value == null) return List.of("Echo Time HUD has no rendered bounds yet.");
        Minecraft minecraft = Minecraft.getInstance();
        double sx = (double) minecraft.getWindow().getWidth() / minecraft.getWindow().getGuiScaledWidth();
        double sy = (double) minecraft.getWindow().getHeight() / minecraft.getWindow().getGuiScaledHeight();
        return List.of("framebuffer=" + rect(value.framebuffer()) + " reference=" + rect(value.referenceCanvas()),
                "uniformScale=" + decimal(value.uniformScale()) + " safe=" + rect(value.safeRect()),
                "anchor=" + configs.current().anchor() + " text=\"" + lastText + "\" bounds=" + rect(value.rawTextRect()),
                "finalPhysical=" + rect(value.finalTextRect()) + " gui=" + decimal(value.finalTextRect().x() / sx)
                        + "," + decimal(value.finalTextRect().y() / sy) + " guiScale=" + decimal(sx) + "x" + decimal(sy));
    }

    private String rect(PhysicalRect value) {
        return decimal(value.x()) + "," + decimal(value.y()) + " " + decimal(value.width()) + "x" + decimal(value.height());
    }
    private String decimal(double value) { return String.format(Locale.ROOT, "%.3f", value); }
}

