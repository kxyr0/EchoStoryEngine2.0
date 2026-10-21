package io.github.kxyr0.echowardrobe.core.catalog;

import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable profile catalog.
 */
public final class WardrobeProfileCatalog {
    private final List<WardrobeProfileDefinition> definitions;
    private final Map<WardrobeProfileId, WardrobeProfileDefinition> byId;

    private WardrobeProfileCatalog(List<WardrobeProfileDefinition> definitions) {
        this.definitions = List.copyOf(definitions);
        Map<WardrobeProfileId, WardrobeProfileDefinition> index = new LinkedHashMap<>();
        definitions.forEach(profile -> index.putIfAbsent(profile.id(), profile));
        this.byId = Map.copyOf(index);
    }

    public static WardrobeProfileCatalog of(List<WardrobeProfileDefinition> definitions) {
        return new WardrobeProfileCatalog(definitions);
    }

    public static WardrobeProfileCatalog empty() {
        return of(List.of());
    }

    public List<WardrobeProfileDefinition> definitions() {
        return definitions;
    }

    public Optional<WardrobeProfileDefinition> find(WardrobeProfileId id) {
        return Optional.ofNullable(byId.get(id));
    }
}
