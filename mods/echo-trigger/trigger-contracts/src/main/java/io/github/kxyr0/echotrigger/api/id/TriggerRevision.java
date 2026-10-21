package io.github.kxyr0.echotrigger.api.id;

public record TriggerRevision(long value) {
    public TriggerRevision {
        if (value < 0) {
            throw new IllegalArgumentException("TriggerRevision must not be negative.");
        }
    }

    public TriggerRevision next() {
        return new TriggerRevision(value + 1);
    }
}
