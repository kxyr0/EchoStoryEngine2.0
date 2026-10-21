package io.github.kxyr0.echothoughtspace.core.canvas;

public record CanvasViewport(int framebufferWidth, int framebufferHeight, int guiWidth, int guiHeight) {
    public CanvasViewport {
        if (framebufferWidth < 1 || framebufferHeight < 1 || guiWidth < 1 || guiHeight < 1) {
            throw new IllegalArgumentException("Viewport dimensions must be positive.");
        }
    }

    public double guiToPhysicalX(double value) { return value * framebufferWidth / guiWidth; }
    public double guiToPhysicalY(double value) { return value * framebufferHeight / guiHeight; }
    public float physicalToGuiScaleX() { return guiWidth / (float) framebufferWidth; }
    public float physicalToGuiScaleY() { return guiHeight / (float) framebufferHeight; }
}

