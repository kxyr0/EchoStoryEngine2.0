package io.github.kxyr0.echoobservation.forge.client.presentation;

public final class ObservationDistanceFade {
    private static final double FRACTION = 0.25D;
    private static final double MINIMUM_RANGE = 1.0D;

    private ObservationDistanceFade() {
    }

    public static double opacity(double distance, double maximum) {
        if (maximum <= 0.0D || distance >= maximum) return 0.0D;
        double fadeRange = Math.min(maximum, Math.max(MINIMUM_RANGE, maximum * FRACTION));
        double linear = Math.min(1.0D, Math.max(0.0D, (maximum - distance) / fadeRange));
        return linear * linear * (3.0D - 2.0D * linear);
    }
}
