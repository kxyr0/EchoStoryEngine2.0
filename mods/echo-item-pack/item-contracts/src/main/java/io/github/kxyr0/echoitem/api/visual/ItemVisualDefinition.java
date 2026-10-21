package io.github.kxyr0.echoitem.api.visual;

import java.util.Objects;
import java.util.Optional;

/**
 * Authored or generated item visual resource references.
 */
public record ItemVisualDefinition(
        String texture,
        Optional<String> model,
        boolean handheld,
        long assetRevision
) {
    public ItemVisualDefinition {
        texture = requireResource(texture, "texture");
        model = Objects.requireNonNull(model, "model");
        model.ifPresent(value -> requireResource(value, "model"));
        if (assetRevision < 0L) {
            throw new IllegalArgumentException("Asset revision must not be negative.");
        }
    }

    public static ItemVisualDefinition handheld(String texture, long assetRevision) {
        return new ItemVisualDefinition(texture, Optional.empty(), true, assetRevision);
    }

    private static String requireResource(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank() || value.contains("..") || value.contains("\\")) {
            throw new IllegalArgumentException(field + " must be a safe resource path.");
        }
        return value;
    }
}
