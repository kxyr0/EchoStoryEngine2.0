package io.github.kxyr0.echowardrobe.core.catalog;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable catalog of wearable definitions.
 */
public final class WearableCatalog {
    private final List<WearableDefinition> definitions;
    private final Map<String, WearableDefinition> activeByItemId;
    private final Map<WearableId, WearableDefinition> byId;
    private final long revision;

    private WearableCatalog(List<WearableDefinition> definitions, long revision) {
        this.definitions = List.copyOf(definitions);
        this.revision = revision;
        this.byId = idIndex(definitions);
        this.activeByItemId = itemIndex(definitions);
    }

    public static WearableCatalog empty() {
        return new WearableCatalog(List.of(), 0L);
    }

    public static WardrobeResult<WearableCatalog> of(List<WearableDefinition> definitions, long revision) {
        List<WardrobeDiagnostic> diagnostics = duplicateDiagnostics(definitions);
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        return WardrobeResult.valid(new WearableCatalog(definitions, revision), diagnostics);
    }

    public List<WearableDefinition> definitions() {
        return definitions;
    }

    public long revision() {
        return revision;
    }

    public int count() {
        return definitions.size();
    }

    public Optional<WearableDefinition> find(WearableId id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<WearableDefinition> activeByItemId(String itemId) {
        return Optional.ofNullable(activeByItemId.get(Objects.requireNonNull(itemId, "itemId")));
    }

    public List<WearableDefinition> byProfileAndSlot(WardrobeProfileId profileId, WardrobeSlot slot) {
        return definitions.stream()
                .filter(definition -> definition.profileId().equals(profileId))
                .filter(definition -> definition.slot() == slot)
                .toList();
    }

    private static Map<WearableId, WearableDefinition> idIndex(List<WearableDefinition> definitions) {
        Map<WearableId, WearableDefinition> index = new LinkedHashMap<>();
        definitions.forEach(definition -> index.putIfAbsent(definition.id(), definition));
        return Map.copyOf(index);
    }

    private static Map<String, WearableDefinition> itemIndex(List<WearableDefinition> definitions) {
        Map<String, WearableDefinition> index = new LinkedHashMap<>();
        definitions.stream()
                .filter(WearableDefinition::active)
                .forEach(definition -> index.putIfAbsent(definition.itemBinding().itemId(), definition));
        return Map.copyOf(index);
    }

    private static List<WardrobeDiagnostic> duplicateDiagnostics(List<WearableDefinition> definitions) {
        Map<WearableId, Integer> counts = new LinkedHashMap<>();
        definitions.forEach(definition -> counts.merge(definition.id(), 1, Integer::sum));
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>();
        counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .forEach(entry -> diagnostics.add(WardrobeDiagnostic.error(
                        WardrobeDiagnosticCode.WARDROBE_WEARABLE_DUPLICATE,
                        "Wearable ID is duplicated: " + entry.getKey(), entry.getKey().toString())));
        return diagnostics;
    }
}
