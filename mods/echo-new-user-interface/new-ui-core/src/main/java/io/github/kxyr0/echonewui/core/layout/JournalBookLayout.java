package io.github.kxyr0.echonewui.core.layout;

public final class JournalBookLayout {
    private final PhysicalToGuiMapper mapper = new PhysicalToGuiMapper();

    public JournalBookLayoutResult layout(UiViewport viewport, UiLayoutConfig config) {
        double scale = fitScale(viewport, config);
        double bookWidth = config.referenceBookWidth() * scale;
        double bookHeight = config.referenceBookHeight() * scale;
        double left = (viewport.framebufferWidth() - bookWidth) / 2.0D;
        double top = (viewport.framebufferHeight() - bookHeight) / 2.0D;
        UiRectangle physical = roundedRect(left, top, left + bookWidth, top + bookHeight);
        validatePhysical(viewport, physical);
        return new JournalBookLayoutResult(viewport, scale, physical, mapper.map(physical, viewport));
    }

    private static double fitScale(UiViewport viewport, UiLayoutConfig config) {
        UiReferenceCanvas canvas = config.referenceCanvas();
        int safeWidth = config.referenceBookWidth() + 2 * config.referenceSafeMargin();
        int safeHeight = config.referenceBookHeight() + 2 * config.referenceSafeMargin();
        double scale = min(
                viewport.framebufferWidth() / (double) canvas.width(),
                viewport.framebufferHeight() / (double) canvas.height(),
                viewport.framebufferWidth() / (double) safeWidth,
                viewport.framebufferHeight() / (double) safeHeight);
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            throw new IllegalArgumentException("layout scale is invalid: " + scale);
        }
        return scale;
    }

    private static UiRectangle roundedRect(double left, double top, double right, double bottom) {
        return UiRectangle.fromEdges(roundEdge(left), roundEdge(top), roundEdge(right), roundEdge(bottom));
    }

    private static int roundEdge(double value) {
        return (int) Math.round(value);
    }

    private static void validatePhysical(UiViewport viewport, UiRectangle physical) {
        if (physical.clippedBy(viewport.framebufferWidth(), viewport.framebufferHeight())) {
            throw new IllegalArgumentException("journal book layout is clipped: " + physical);
        }
    }

    private static double min(double first, double second, double third, double fourth) {
        return Math.min(Math.min(first, second), Math.min(third, fourth));
    }
}
