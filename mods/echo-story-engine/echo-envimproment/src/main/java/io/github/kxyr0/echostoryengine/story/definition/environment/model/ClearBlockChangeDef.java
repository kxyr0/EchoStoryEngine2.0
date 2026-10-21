package io.github.kxyr0.echostoryengine.story.definition.environment.model;

import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.util.Objects;

/**
 * Replaces a controlled position with air.
 */
public record ClearBlockChangeDef(
        EnvironmentPosition position,
        SourceLocation location) implements EnvironmentChangeDef {
    public ClearBlockChangeDef {
        position = Objects.requireNonNull(position, "position");
        location = Objects.requireNonNull(location, "location");
    }
}
