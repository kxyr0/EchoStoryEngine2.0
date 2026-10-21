package io.github.kxyr0.echostoryengine.story.definition.environment.model;

import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.util.Objects;

/**
 * One authored Minecraft block state property assignment.
 */
public record BlockPropertyDef(String name, String value, SourceLocation location) {
    public BlockPropertyDef {
        name = requireText(name, "name");
        value = requireText(value, "value");
        location = Objects.requireNonNull(location, "location");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
