package io.github.kxyr0.echonewui.core.layout;

public final class PhysicalToGuiMapper {
    public UiRectangle map(UiRectangle physical, UiViewport viewport) {
        int left = mapEdge(physical.x(), viewport.guiWidth(), viewport.framebufferWidth());
        int top = mapEdge(physical.y(), viewport.guiHeight(), viewport.framebufferHeight());
        int right = mapEdge(physical.right(), viewport.guiWidth(), viewport.framebufferWidth());
        int bottom = mapEdge(physical.bottom(), viewport.guiHeight(), viewport.framebufferHeight());
        return UiRectangle.fromEdges(left, top, growIfEmpty(left, right, viewport.guiWidth()),
                growIfEmpty(top, bottom, viewport.guiHeight()));
    }

    public UiRectangle backToPhysical(UiRectangle gui, UiViewport viewport) {
        int left = mapEdge(gui.x(), viewport.framebufferWidth(), viewport.guiWidth());
        int top = mapEdge(gui.y(), viewport.framebufferHeight(), viewport.guiHeight());
        int right = mapEdge(gui.right(), viewport.framebufferWidth(), viewport.guiWidth());
        int bottom = mapEdge(gui.bottom(), viewport.framebufferHeight(), viewport.guiHeight());
        return UiRectangle.fromEdges(left, top, growIfEmpty(left, right, viewport.framebufferWidth()),
                growIfEmpty(top, bottom, viewport.framebufferHeight()));
    }

    private static int mapEdge(int edge, int targetSize, int sourceSize) {
        return (int) Math.round(edge * targetSize / (double) sourceSize);
    }

    private static int growIfEmpty(int start, int end, int boundary) {
        if (end > start) {
            return end;
        }
        if (start < boundary) {
            return start + 1;
        }
        return boundary;
    }
}
