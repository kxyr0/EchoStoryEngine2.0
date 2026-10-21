package io.github.kxyr0.echomagic.api.view;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import java.util.List;
import java.util.Objects;

public record SpellCatalogView(List<SpellDefinition> definitions, long revision) {
    public SpellCatalogView {
        definitions = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
        if (revision < 0L) throw new IllegalArgumentException("Revision must not be negative.");
    }
}

