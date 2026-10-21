package io.github.kxyr0.echonpc.api.form.definition;

public record NpcAppearanceRevision(long value) {
    public NpcAppearanceRevision {
        if (value < 0L) {
            throw new IllegalArgumentException("NpcAppearanceRevision must not be negative.");
        }
    }
}
