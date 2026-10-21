package io.github.kxyr0.echoanimationeditor.api.viewport;

public record AnimatorPanelRect(int left, int top, int width, int height) {
    public static final AnimatorPanelRect EMPTY = new AnimatorPanelRect(0, 0, 0, 0);

    public AnimatorPanelRect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("panel size is negative");
        }
    }

    public int right() {
        return left + width;
    }

    public int bottom() {
        return top + height;
    }

    public boolean contains(int x, int y) {
        return x >= left && x < right() && y >= top && y < bottom();
    }

    public boolean overlaps(AnimatorPanelRect other) {
        return width > 0 && height > 0 && other.width > 0 && other.height > 0
                && left < other.right() && right() > other.left
                && top < other.bottom() && bottom() > other.top;
    }

    public AnimatorPanelRect scaleToGui(AnimatorViewport viewport) {
        int guiLeft = scale(left, viewport.guiWidth(), viewport.framebufferWidth());
        int guiTop = scale(top, viewport.guiHeight(), viewport.framebufferHeight());
        int guiRight = scale(right(), viewport.guiWidth(), viewport.framebufferWidth());
        int guiBottom = scale(bottom(), viewport.guiHeight(), viewport.framebufferHeight());
        return new AnimatorPanelRect(guiLeft, guiTop, guiRight - guiLeft, guiBottom - guiTop);
    }

    private static int scale(int value, int target, int source) {
        if (source <= 0) {
            return 0;
        }
        return (int) Math.round((double) value * target / source);
    }
}
