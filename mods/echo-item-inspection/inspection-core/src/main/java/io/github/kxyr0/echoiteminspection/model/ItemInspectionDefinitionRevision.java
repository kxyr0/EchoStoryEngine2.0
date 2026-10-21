package io.github.kxyr0.echoiteminspection.model;

/** Catalog generation pinned by an inspection session. */
public record ItemInspectionDefinitionRevision(long value) {
    public ItemInspectionDefinitionRevision {
        if (value < 0) throw new IllegalArgumentException("Negative inspection revision.");
    }
}
