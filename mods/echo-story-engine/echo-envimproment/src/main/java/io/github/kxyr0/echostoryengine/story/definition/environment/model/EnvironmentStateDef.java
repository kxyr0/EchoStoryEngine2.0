package io.github.kxyr0.echostoryengine.story.definition.environment.model;

import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.util.List;
import java.util.Objects;

/**
 * Complete authored state for all positions this state controls.
 */
public record EnvironmentStateDef(
        String id,
        List<EnvironmentChangeDef> changes,
        SourceLocation location) {
    public EnvironmentStateDef {
        id = requireText(id, "id");
        changes = List.copyOf(Objects.requireNonNull(changes, "changes"));
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
