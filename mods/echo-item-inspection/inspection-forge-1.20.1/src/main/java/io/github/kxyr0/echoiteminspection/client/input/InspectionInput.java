package io.github.kxyr0.echoiteminspection.client.input;

import io.github.kxyr0.echoiteminspection.math.ItemInspectionTransform;

/** Mouse ownership is separate from presentation math; only a held LMB changes rotation. */
public final class InspectionInput {
    private final ItemInspectionTransform transform;
    private boolean dragging;

    public InspectionInput(ItemInspectionTransform transform) {
        this.transform = transform;
    }

    public void press(int button) {
        if (button == 0) dragging = true;
    }

    public void release(int button) {
        if (button == 0) dragging = false;
    }

    public boolean drag(int button, double deltaX, double deltaY, double guiScale) {
        if (!dragging || button != 0) return false;
        transform.drag(deltaX * guiScale, deltaY * guiScale);
        return true;
    }

    public void scroll(double amount) {
        transform.scroll(amount);
    }

    public void cancelDrag() {
        dragging = false;
    }
}
