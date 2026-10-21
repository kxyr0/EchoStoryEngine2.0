package io.github.kxyr0.echowardrobe.core.catalog;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantStatus;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable exact OutfitKey catalog with O(1) lookup.
 */
public final class OutfitVariantCatalog {
    private final List<OutfitVariantDefinition> definitions;
    private final Map<OutfitVariantId, OutfitVariantDefinition> byId;
    private final Map<OutfitKey, OutfitVariantDefinition> activeByKey;

    private OutfitVariantCatalog(List<OutfitVariantDefinition> definitions) {
        this.definitions = List.copyOf(definitions);
        this.byId = idIndex(definitions);
        this.activeByKey = keyIndex(definitions);
    }

    public static WardrobeResult<OutfitVariantCatalog> of(List<OutfitVariantDefinition> definitions) {
        List<WardrobeDiagnostic> diagnostics = duplicateKeyDiagnostics(definitions);
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        return WardrobeResult.valid(new OutfitVariantCatalog(definitions), diagnostics);
    }

    public static OutfitVariantCatalog empty() {
        return new OutfitVariantCatalog(List.of());
    }

    public List<OutfitVariantDefinition> definitions() {
        return definitions;
    }

    public Optional<OutfitVariantDefinition> find(OutfitVariantId id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<OutfitVariantDefinition> exact(OutfitKey key) {
        return Optional.ofNullable(activeByKey.get(key));
    }

    public List<OutfitVariantDefinition> byProfile(WardrobeProfileId id) {
        return definitions.stream().filter(variant -> variant.profileId().equals(id)).toList();
    }

    public boolean indexedLookup() {
        return true;
    }

    private static Map<OutfitVariantId, OutfitVariantDefinition> idIndex(
            List<OutfitVariantDefinition> definitions) {
        Map<OutfitVariantId, OutfitVariantDefinition> index = new LinkedHashMap<>();
        definitions.forEach(variant -> index.putIfAbsent(variant.id(), variant));
        return Map.copyOf(index);
    }

    private static Map<OutfitKey, OutfitVariantDefinition> keyIndex(List<OutfitVariantDefinition> definitions) {
        Map<OutfitKey, OutfitVariantDefinition> index = new LinkedHashMap<>();
        definitions.stream()
                .filter(variant -> variant.status() == OutfitVariantStatus.ACTIVE)
                .forEach(variant -> index.putIfAbsent(variant.key(), variant));
        return Map.copyOf(index);
    }

    private static List<WardrobeDiagnostic> duplicateKeyDiagnostics(List<OutfitVariantDefinition> definitions) {
        Map<OutfitKey, Integer> counts = new LinkedHashMap<>();
        definitions.stream()
                .filter(variant -> variant.status() == OutfitVariantStatus.ACTIVE)
                .forEach(variant -> counts.merge(variant.key(), 1, Integer::sum));
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>();
        counts.entrySet().stream().filter(entry -> entry.getValue() > 1).forEach(entry ->
                diagnostics.add(WardrobeDiagnostic.error(WardrobeDiagnosticCode.WARDROBE_OUTFIT_KEY_DUPLICATE,
                        "Exact OutfitKey is duplicated: " + entry.getKey().serialize(),
                        entry.getKey().profileId().toString())));
        return diagnostics;
    }
}
