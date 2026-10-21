package io.github.kxyr0.echoobservation.forge.client.projection;

public record ObservationScreenPoint(double framebufferX, double framebufferY, double depth, double ndcX,
        double ndcY) {
}
