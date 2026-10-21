package io.github.kxyr0.echoiteminspection.world;

/** Same 1920x1080 reference-canvas/min-axis convention as Echo Player HUD. */
public record WorldInspectionPromptLayout(double x, double y, double width, double height, double scale) {
    public static WorldInspectionPromptLayout resolve(int framebufferWidth, int framebufferHeight, int textWidth) {
        return resolve(framebufferWidth, framebufferHeight, textWidth, 1, 0, 36);
    }

    public static WorldInspectionPromptLayout resolve(int framebufferWidth, int framebufferHeight, int textWidth,
            double userScale, double minimumWidth, double panelHeight) {
        if (framebufferWidth < 1 || framebufferHeight < 1 || textWidth < 0) {
            throw new IllegalArgumentException("Invalid prompt dimensions");
        }
        if (!Double.isFinite(userScale) || userScale <= 0 || !Double.isFinite(minimumWidth)
                || minimumWidth < 0 || !Double.isFinite(panelHeight) || panelHeight < 24) {
            throw new IllegalArgumentException("Invalid prompt size settings");
        }
        double resolution = Math.min(framebufferWidth / 1920.0, framebufferHeight / 1080.0);
        double left = Math.round(24 * resolution);
        double bottom = Math.round(32 * resolution);
        double availableWidth = Math.max(1, framebufferWidth - 2 * left);
        double availableHeight = Math.max(1, framebufferHeight - bottom - left);
        double contentWidth = textWidth * 2.0 + 24;
        double scale = Math.min(resolution * userScale,
                Math.min(availableWidth / contentWidth, availableHeight / panelHeight));
        double height = Math.max(1, Math.round(panelHeight * scale));
        double width = Math.min(availableWidth, Math.max(1, Math.round(Math.max(contentWidth, minimumWidth) * scale)));
        return new WorldInspectionPromptLayout(left, framebufferHeight - bottom - height, width, height, scale);
    }
}
