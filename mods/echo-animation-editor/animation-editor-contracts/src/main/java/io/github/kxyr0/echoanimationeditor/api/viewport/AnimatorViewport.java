package io.github.kxyr0.echoanimationeditor.api.viewport;

public record AnimatorViewport(
        int framebufferWidth,
        int framebufferHeight,
        int guiWidth,
        int guiHeight,
        double guiScale,
        long windowRevision
) {
    public AnimatorViewport {
        if (framebufferWidth <= 0 || framebufferHeight <= 0 || guiWidth <= 0 || guiHeight <= 0) {
            throw new IllegalArgumentException("viewport dimensions must be positive");
        }
        if (guiScale <= 0.0D) {
            throw new IllegalArgumentException("guiScale must be positive");
        }
    }

    public int guiToPhysicalX(double guiX) {
        return (int) Math.round(guiX * framebufferWidth / guiWidth);
    }

    public int guiToPhysicalY(double guiY) {
        return (int) Math.round(guiY * framebufferHeight / guiHeight);
    }
}
