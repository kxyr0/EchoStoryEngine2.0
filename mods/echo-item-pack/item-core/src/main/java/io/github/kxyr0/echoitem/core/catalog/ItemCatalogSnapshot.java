package io.github.kxyr0.echoitem.core.catalog;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable server-authoritative item definition catalog.
 */
public record ItemCatalogSnapshot(
        ItemDefinitionRevision revision,
        Map<EchoItemId, WeaponDefinition> weapons
) {
    public ItemCatalogSnapshot {
        revision = Objects.requireNonNull(revision, "revision");
        weapons = copySorted(weapons);
    }

    public static ItemCatalogSnapshot empty() {
        return new ItemCatalogSnapshot(ItemDefinitionRevision.ZERO, Map.of());
    }

    public Optional<WeaponDefinition> weapon(EchoItemId id) {
        return Optional.ofNullable(weapons.get(id));
    }

    public List<EchoItemId> weaponIds() {
        return weapons.keySet().stream().sorted().toList();
    }

    public ItemCatalogSnapshot withWeapons(Collection<WeaponDefinition> definitions) {
        Map<EchoItemId, WeaponDefinition> merged = new LinkedHashMap<>(weapons);
        definitions.forEach(definition -> merged.put(definition.id(), definition));
        return new ItemCatalogSnapshot(revision.next(), merged);
    }

    private static Map<EchoItemId, WeaponDefinition> copySorted(Map<EchoItemId, WeaponDefinition> values) {
        Objects.requireNonNull(values, "values");
        Map<EchoItemId, WeaponDefinition> sorted = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().value()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return Map.copyOf(sorted);
    }
}
