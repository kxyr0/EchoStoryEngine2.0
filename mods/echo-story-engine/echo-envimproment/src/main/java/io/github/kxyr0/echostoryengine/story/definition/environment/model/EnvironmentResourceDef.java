package io.github.kxyr0.echostoryengine.story.definition.environment.model;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable compiled environment resource definition.
 */
public record EnvironmentResourceDef(
        NamespacedId id,
        NamespacedId dimension,
        List<EnvironmentStateDef> states,
        SourceLocation location) {
    public EnvironmentResourceDef {
        id = Objects.requireNonNull(id, "id");
        dimension = Objects.requireNonNull(dimension, "dimension");
        states = List.copyOf(Objects.requireNonNull(states, "states"));
        location = Objects.requireNonNull(location, "location");
    }

    public Optional<EnvironmentStateDef> findState(String stateId) {
        return states.stream()
                .filter(state -> state.id().equals(stateId))
                .findFirst();
    }
}
