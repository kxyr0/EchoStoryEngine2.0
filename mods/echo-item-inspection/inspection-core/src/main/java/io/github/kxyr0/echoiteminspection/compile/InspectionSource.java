package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoiteminspection.model.ItemInspectionId;
import java.util.Objects;

/** Canonical source identity carries the content namespace without requiring a namespace declaration. */
public record InspectionSource(String sourceId, String text) {
    public InspectionSource {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(text, "text");
    }

    public ItemInspectionId inferredId() {
        String normalized = sourceId.replace('\\', '/');
        String[] parts = normalized.split("/");
        String namespace = null;
        for (int index = 0; index + 1 < parts.length; index++) {
            if (parts[index].equals("data") || parts[index].equals("assets")) namespace = parts[index + 1];
        }
        String filename = parts[parts.length - 1];
        String basename = filename.replaceFirst("\\.inspection\\.es$", "");
        if (namespace == null || basename.equals(filename)) {
            throw new IllegalArgumentException("Inspection source requires data/<namespace>/.../<name>.inspection.es.");
        }
        return new ItemInspectionId(namespace + ":" + basename);
    }
}
