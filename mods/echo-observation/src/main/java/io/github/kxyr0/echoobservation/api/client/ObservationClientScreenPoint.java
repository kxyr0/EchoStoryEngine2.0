package io.github.kxyr0.echoobservation.api.client;

public record ObservationClientScreenPoint(double framebufferX, double framebufferY, double depth) {
    public ObservationClientScreenPoint {
        if (!Double.isFinite(framebufferX) || !Double.isFinite(framebufferY)
                || !Double.isFinite(depth) || depth <= 0.0D) {
            throw new IllegalArgumentException("Invalid Observation client screen point.");
        }
    }
}
