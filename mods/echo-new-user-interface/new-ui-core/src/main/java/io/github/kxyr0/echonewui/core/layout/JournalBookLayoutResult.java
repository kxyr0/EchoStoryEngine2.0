package io.github.kxyr0.echonewui.core.layout;

public record JournalBookLayoutResult(
        UiViewport viewport,
        double scale,
        UiRectangle physicalRect,
        UiRectangle guiRect) {

    public JournalBookLayoutResult {
        if (viewport == null || physicalRect == null || guiRect == null) {
            throw new IllegalArgumentException("layout result fields must not be null");
        }
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            throw new IllegalArgumentException("scale must be finite and positive: " + scale);
        }
    }

    public double centerErrorX() {
        return Math.abs(physicalRect.centerX() - viewport.framebufferWidth() / 2.0D);
    }

    public double centerErrorY() {
        return Math.abs(physicalRect.centerY() - viewport.framebufferHeight() / 2.0D);
    }

    public boolean clipped() {
        return physicalRect.clippedBy(viewport.framebufferWidth(), viewport.framebufferHeight());
    }
}
