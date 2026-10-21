package io.github.kxyr0.echoconcentration.api;

public record ConcentrationRevision(long value) {
    public ConcentrationRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("Concentration revision must not be negative.");
        }
    }

    public ConcentrationRevision next() {
        return new ConcentrationRevision(Math.addExact(value, 1L));
    }
}
