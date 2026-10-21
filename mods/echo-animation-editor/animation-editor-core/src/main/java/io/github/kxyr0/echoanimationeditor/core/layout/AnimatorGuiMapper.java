package io.github.kxyr0.echoanimationeditor.core.layout;

import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorViewport;

public final class AnimatorGuiMapper {
    public AnimatorPanelRect toGui(AnimatorPanelRect physical, AnimatorViewport viewport) {
        return physical.scaleToGui(viewport);
    }

    public int physicalX(double guiX, AnimatorViewport viewport) {
        return viewport.guiToPhysicalX(guiX);
    }

    public int physicalY(double guiY, AnimatorViewport viewport) {
        return viewport.guiToPhysicalY(guiY);
    }
}
