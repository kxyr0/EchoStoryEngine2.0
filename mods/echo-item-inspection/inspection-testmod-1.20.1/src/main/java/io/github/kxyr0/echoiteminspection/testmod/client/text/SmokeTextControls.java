package io.github.kxyr0.echoiteminspection.testmod.client.text;

import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import io.github.kxyr0.echoiteminspection.testmod.client.SmokeEvidence;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

final class SmokeTextControls {
    private SmokeTextControls() { }

    static void check(ItemInspectionScreen viewer, SmokeEvidence evidence) {
        var panel = viewer.textPanel();
        var transform = viewer.session().transform();
        var orientation = transform.orientation();
        evidence.check("longDescriptionScrollable", panel.scroll().canScroll());
        evidence.check("longDescriptionWrapped", panel.lineCount() > panel.scroll().visibleLines());
        viewer.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0);
        evidence.check("descriptionPageDown", panel.scroll().position() > 0);
        int position = panel.scroll().position();
        double beforeZoom = transform.zoom();
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        var layout = panel.layout();
        double x = (layout.textX() + layout.textWidth()) / scale;
        double top = layout.textY() / scale;
        double bottom = (layout.textY() + layout.textHeight()) / scale;
        viewer.mouseScrolled(x, top, 1);
        evidence.check("wheelOverDescriptionStillZooms", transform.zoom() > beforeZoom
                && panel.scroll().position() == position);
        viewer.mouseClicked(x, top, 0);
        viewer.mouseDragged(x, bottom, 0, 0, bottom - top);
        viewer.mouseReleased(x, bottom, 0);
        evidence.check("descriptionScrollbarReachesEnd", panel.scroll().position() == panel.scroll().maximum());
        evidence.check("descriptionReadingDoesNotRotate", transform.orientation().equals(orientation));
        viewer.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
        evidence.check("descriptionHomeReturnsToStart", panel.scroll().position() == 0);
        transform.reset();
    }
}
