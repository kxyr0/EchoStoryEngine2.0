package io.github.kxyr0.echoanimationeditor.core.layout;

import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorLayout;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import java.util.ArrayList;
import java.util.List;

public final class AnimatorLayoutValidation {
    public List<String> validate(AnimatorLayout layout, int width, int height) {
        List<String> violations = new ArrayList<>();
        List<AnimatorPanelRect> panels = layout.visiblePanels();
        for (AnimatorPanelRect panel : panels) {
            validatePanel(width, height, violations, panel);
        }
        for (int first = 0; first < panels.size(); first++) {
            validateOverlaps(violations, panels, first);
        }
        return violations;
    }

    private static void validatePanel(int width, int height, List<String> violations, AnimatorPanelRect panel) {
        if (panel.width() <= 0 || panel.height() <= 0) {
            violations.add("negative-or-empty panel " + panel);
        }
        if (panel.left() < 0 || panel.top() < 0 || panel.right() > width || panel.bottom() > height) {
            violations.add("overflow panel " + panel);
        }
    }

    private static void validateOverlaps(List<String> violations, List<AnimatorPanelRect> panels, int first) {
        for (int second = first + 1; second < panels.size(); second++) {
            if (panels.get(first).overlaps(panels.get(second))) {
                violations.add("overlap " + panels.get(first) + " " + panels.get(second));
            }
        }
    }
}
