package io.github.kxyr0.echoobservation.api.model;

public record ObservationDuration(int ticks) {
    public ObservationDuration {
        if (ticks < 1 || ticks > 20 * 60) {
            throw new IllegalArgumentException("Observation focus duration must be 1..1200 ticks.");
        }
    }

    public static ObservationDuration seconds(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0.0D || seconds > 60.0D) {
            throw new IllegalArgumentException("Focus seconds must be finite and in (0, 60].");
        }
        return new ObservationDuration(Math.max(1, (int) Math.ceil(seconds * 20.0D)));
    }

    public double seconds() {
        return ticks / 20.0D;
    }
}
