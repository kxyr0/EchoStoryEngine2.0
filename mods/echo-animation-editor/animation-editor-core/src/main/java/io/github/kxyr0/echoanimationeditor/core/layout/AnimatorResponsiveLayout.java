package io.github.kxyr0.echoanimationeditor.core.layout;

import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorLayout;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorLayoutMode;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorViewport;

public final class AnimatorResponsiveLayout {
    public AnimatorLayout layout(AnimatorViewport viewport) {
        if (viewport.framebufferWidth() < 900 || viewport.framebufferHeight() < 560) {
            return tiny(viewport);
        }
        if (viewport.framebufferWidth() < 1200 || viewport.framebufferHeight() < 720) {
            return compact(viewport);
        }
        return desktop(viewport);
    }

    private AnimatorLayout desktop(AnimatorViewport viewport) {
        double sx = viewport.framebufferWidth() / 1920.0D;
        double sy = viewport.framebufferHeight() / 1080.0D;
        int safeX = scale(24, sx);
        int safeY = scale(24, sy);
        int gutter = Math.max(8, scale(12, sx));
        int toolbarHeight = scale(56, sy);
        int mainTop = scale(92, sy);
        int inspectorWidth = scale(360, sx);
        int timelineHeight = scale(270, sy);
        AnimatorPanelRect toolbar = new AnimatorPanelRect(safeX, safeY,
                viewport.framebufferWidth() - safeX * 2, toolbarHeight);
        AnimatorPanelRect inspector = new AnimatorPanelRect(
                viewport.framebufferWidth() - safeX - inspectorWidth,
                mainTop, inspectorWidth, viewport.framebufferHeight() - mainTop - safeY);
        AnimatorPanelRect timeline = new AnimatorPanelRect(safeX,
                viewport.framebufferHeight() - safeY - timelineHeight,
                inspector.left() - gutter - safeX, timelineHeight);
        AnimatorPanelRect world = new AnimatorPanelRect(safeX, mainTop, timeline.width(),
                timeline.top() - gutter - mainTop);
        return new AnimatorLayout(AnimatorLayoutMode.DESKTOP, toolbar, world, timeline, inspector, false);
    }

    private AnimatorLayout compact(AnimatorViewport viewport) {
        int safe = 12;
        int gutter = 8;
        int toolbarHeight = 44;
        int timelineHeight = clamp((int) Math.round(viewport.framebufferHeight() * 0.24D), 104, 180);
        int inspectorWidth = clamp((int) Math.round(viewport.framebufferWidth() * 0.28D), 220, 300);
        int mainTop = safe + toolbarHeight + gutter;
        AnimatorPanelRect toolbar = new AnimatorPanelRect(safe, safe,
                viewport.framebufferWidth() - safe * 2, toolbarHeight);
        AnimatorPanelRect inspector = new AnimatorPanelRect(
                viewport.framebufferWidth() - safe - inspectorWidth, mainTop,
                inspectorWidth, viewport.framebufferHeight() - mainTop - timelineHeight - safe - gutter);
        AnimatorPanelRect timeline = new AnimatorPanelRect(safe,
                viewport.framebufferHeight() - safe - timelineHeight,
                inspector.left() - gutter - safe, timelineHeight);
        AnimatorPanelRect world = new AnimatorPanelRect(safe, mainTop, timeline.width(),
                timeline.top() - gutter - mainTop);
        return new AnimatorLayout(AnimatorLayoutMode.COMPACT, toolbar, world, timeline, inspector, false);
    }

    private AnimatorLayout tiny(AnimatorViewport viewport) {
        int safe = 8;
        int gutter = 6;
        int toolbarHeight = 36;
        int timelineHeight = clamp((int) Math.round(viewport.framebufferHeight() * 0.22D), 80, 140);
        int mainTop = safe + toolbarHeight + gutter;
        AnimatorPanelRect toolbar = new AnimatorPanelRect(safe, safe,
                viewport.framebufferWidth() - safe * 2, toolbarHeight);
        AnimatorPanelRect timeline = new AnimatorPanelRect(safe,
                viewport.framebufferHeight() - safe - timelineHeight,
                viewport.framebufferWidth() - safe * 2, timelineHeight);
        AnimatorPanelRect world = new AnimatorPanelRect(safe, mainTop, timeline.width(),
                timeline.top() - gutter - mainTop);
        return new AnimatorLayout(AnimatorLayoutMode.TINY, toolbar, world, timeline,
                AnimatorPanelRect.EMPTY, true);
    }

    private static int scale(int value, double factor) {
        return Math.max(1, (int) Math.round(value * factor));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
