package io.github.kxyr0.echoitem.core.reload;

import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.core.source.ItemSourceOrigin;
import java.util.Objects;

/**
 * Last-known-good compiled candidate with source precedence metadata.
 */
public record CompiledWeaponCandidate(
        String sourceId,
        ItemSourceOrigin origin,
        WeaponDefinition definition
) {
    public CompiledWeaponCandidate {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        origin = Objects.requireNonNull(origin, "origin");
        definition = Objects.requireNonNull(definition, "definition");
    }
}
