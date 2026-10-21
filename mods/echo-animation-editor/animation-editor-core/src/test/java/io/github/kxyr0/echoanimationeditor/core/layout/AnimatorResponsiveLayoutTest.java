package io.github.kxyr0.echoanimationeditor.core.layout;

import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorLayout;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorViewport;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AnimatorResponsiveLayoutTest {
    private final AnimatorResponsiveLayout layout = new AnimatorResponsiveLayout();
    private final AnimatorLayoutValidation validation = new AnimatorLayoutValidation();

    @Test
    void requiredFramebufferMatrixHasNoOverlapOrOverflow() {
        int[][] sizes = {
                {1920, 1080}, {2560, 1440}, {3840, 2160}, {1366, 768}, {1280, 1024},
                {3440, 1440}, {1024, 768}, {960, 540}, {800, 600}, {1080, 1920}, {1919, 1079}
        };
        for (int[] size : sizes) {
            AnimatorLayout result = layout.layout(viewport(size[0], size[1], 2));
            List<String> violations = validation.validate(result, size[0], size[1]);
            assertTrue(violations.isEmpty(), size[0] + "x" + size[1] + " " + violations);
        }
    }

    @Test
    void guiScaleDoesNotChangePhysicalLayout() {
        AnimatorLayout scaleOne = layout.layout(viewport(1920, 1080, 1));
        for (int scale = 2; scale <= 4; scale++) {
            AnimatorLayout other = layout.layout(viewport(1920, 1080, scale));
            assertEquals(scaleOne.toolbar(), other.toolbar());
            assertEquals(scaleOne.viewport(), other.viewport());
            assertEquals(scaleOne.timeline(), other.timeline());
            assertEquals(scaleOne.inspector(), other.inspector());
        }
    }

    private static AnimatorViewport viewport(int width, int height, int scale) {
        return new AnimatorViewport(width, height, width / scale, height / scale, scale, scale);
    }
}
