package io.github.kxyr0.echoiteminspection.math;

import io.github.kxyr0.echoiteminspection.model.ItemInspectionView;
import java.util.Objects;

/** Session-owned presentation transform; input ownership stays outside the rotation and zoom math. */
public final class ItemInspectionTransform {
    private final ItemInspectionView view;
    private QuaternionRotation orientation;
    private double zoom;

    public ItemInspectionTransform(ItemInspectionView view) {
        this.view = Objects.requireNonNull(view, "view");
        reset();
    }

    public ItemInspectionView view() { return view; }

    public QuaternionRotation orientation() { return orientation; }

    public double rotationX() { return orientation.rotationX(); }

    public double rotationY() { return orientation.rotationY(); }

    public double rotationZ() { return orientation.rotationZ(); }

    public double zoom() { return zoom; }

    public void drag(double framebufferDeltaX, double framebufferDeltaY) {
        if (!Double.isFinite(framebufferDeltaX) || !Double.isFinite(framebufferDeltaY)) return;
        QuaternionRotation delta = QuaternionRotation.axisX(degrees(framebufferDeltaY))
                .multiply(QuaternionRotation.axisY(degrees(framebufferDeltaX)));
        orientation = delta.multiply(orientation);
    }

    public void scroll(double amount) {
        if (!Double.isFinite(amount)) return;
        zoom = Math.max(view.minZoom(), Math.min(view.maxZoom(), zoom * Math.exp(amount * view.zoomSensitivity())));
    }

    public void reset() {
        orientation = QuaternionRotation.euler(view.rotationX(), view.rotationY(), view.rotationZ());
        zoom = view.initialZoom();
    }

    private double degrees(double pixels) {
        double fullTurnPixels = 360 / view.rotationSensitivity();
        return (pixels % fullTurnPixels) * view.rotationSensitivity();
    }
}
