package io.github.kxyr0.echoanimationeditor.api.viewport;

import java.util.List;
import java.util.Objects;

public record AnimatorLayout(
        AnimatorLayoutMode mode,
        AnimatorPanelRect toolbar,
        AnimatorPanelRect viewport,
        AnimatorPanelRect timeline,
        AnimatorPanelRect inspector,
        boolean inspectorDrawer
) {
    public AnimatorLayout {
        mode = Objects.requireNonNull(mode, "mode");
        toolbar = Objects.requireNonNull(toolbar, "toolbar");
        viewport = Objects.requireNonNull(viewport, "viewport");
        timeline = Objects.requireNonNull(timeline, "timeline");
        inspector = Objects.requireNonNull(inspector, "inspector");
    }

    public List<AnimatorPanelRect> visiblePanels() {
        if (inspectorDrawer || inspector.width() == 0 || inspector.height() == 0) {
            return List.of(toolbar, viewport, timeline);
        }
        return List.of(toolbar, viewport, timeline, inspector);
    }
}
