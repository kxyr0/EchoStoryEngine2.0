package io.github.kxyr0.echomagic.core.catalog;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record SpellCatalogSnapshot(Map<SpellId, SpellDefinition> byId, long revision) {
    public SpellCatalogSnapshot {
        byId = Map.copyOf(new LinkedHashMap<>(byId));
        if (revision < 0L) throw new IllegalArgumentException("Revision must not be negative.");
    }

    public static SpellCatalogSnapshot empty() { return new SpellCatalogSnapshot(Map.of(), 0L); }
    public Optional<SpellDefinition> find(SpellId id) { return Optional.ofNullable(byId.get(id)); }
    public List<SpellDefinition> definitions() { return byId.values().stream().sorted((a, b) -> a.id().compareTo(b.id())).toList(); }
}

