package io.github.kxyr0.echoobservation.forge.client.presentation;

final class ObservationCenterProgress {
    private ObservationCenterProgress() {
    }

    static int radius(double outerRadius, double progress) {
        if (outerRadius <= 0.0D || progress <= 0.0D) return 0;
        int outerPixels = Math.max(1, (int) Math.ceil(outerRadius));
        if (progress >= 1.0D) return outerPixels;
        return Math.max(1, (int) Math.floor(outerRadius * progress));
    }
}
