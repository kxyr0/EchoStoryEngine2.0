package io.github.kxyr0.echostoryengine.story.definition.environment.model;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Places a real registered block at a controlled position.
 */
public record SetBlockChangeDef(
        EnvironmentPosition position,
        NamespacedId blockId,
        List<BlockPropertyDef> properties,
        Optional<String> blockEntityNbt,
        SourceLocation location) implements EnvironmentChangeDef {
    public SetBlockChangeDef {
        position = Objects.requireNonNull(position, "position");
        blockId = Objects.requireNonNull(blockId, "blockId");
        properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
        blockEntityNbt = Objects.requireNonNull(blockEntityNbt, "blockEntityNbt")
                .map(String::trim)
                .filter(value -> !value.isEmpty());
        location = Objects.requireNonNull(location, "location");
    }
}
