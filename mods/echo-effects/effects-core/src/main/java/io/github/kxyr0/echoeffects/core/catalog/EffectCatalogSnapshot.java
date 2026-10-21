package io.github.kxyr0.echoeffects.core.catalog;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import java.util.List;
import java.util.Objects;

public record EffectCatalogSnapshot(List<EffectDefinition> definitions) {
    public EffectCatalogSnapshot {
        definitions = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
    }
}
