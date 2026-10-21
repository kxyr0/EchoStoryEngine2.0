package io.github.kxyr0.echoitem.core.reload;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import io.github.kxyr0.echoitem.core.source.ItemSourceCandidate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computes changed candidates and applies valid batches atomically.
 */
public final class ItemReloadPlanner {
    private final ItemCandidateCompiler compiler;
    private final WeaponCandidateSelector selector = new WeaponCandidateSelector();
    private final Map<String, ItemSourceFingerprint> fingerprints = new LinkedHashMap<>();
    private final Map<String, CompiledWeaponCandidate> lastGood = new LinkedHashMap<>();

    public ItemReloadPlanner(ItemCandidateCompiler compiler) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public ItemReloadOutcome reload(ItemCatalogSnapshot previous, List<ItemSourceCandidate> candidates) {
        boolean removed = removeMissingSources(candidates);
        List<ItemSourceCandidate> changed = candidates.stream().filter(this::changed).toList();
        if (changed.isEmpty()) {
            if (removed) {
                return publishSelected(previous, List.of(), ReloadCounters.weapon(0));
            }
            return new ItemReloadOutcome(previous, List.of(), ReloadCounters.weapon(0), true);
        }
        var compiled = compiler.compile(changed, previous.revision().next());
        if (compiled.diagnostics().stream().anyMatch(ItemDefinitionDiagnostic::error)) {
            return rejected(previous, compiled);
        }
        var selection = select(compiled.candidates());
        if (selection.diagnostics().stream().anyMatch(ItemDefinitionDiagnostic::error)) {
            return rejected(previous, compiled, selection.diagnostics());
        }
        compiled.candidates().forEach(candidate -> lastGood.put(candidate.sourceId(), candidate));
        changed.forEach(candidate -> fingerprints.put(candidate.sourceId(), candidate.fingerprint()));
        return publishSelected(previous, merge(compiled.diagnostics(), selection.diagnostics()), compiled.counters());
    }

    public void remember(List<ItemSourceCandidate> candidates) {
        candidates.forEach(candidate -> fingerprints.put(candidate.sourceId(), candidate.fingerprint()));
    }

    public List<CompiledWeaponCandidate> lastGoodCandidates() {
        return List.copyOf(lastGood.values());
    }

    private boolean changed(ItemSourceCandidate candidate) {
        return !candidate.fingerprint().equals(fingerprints.get(candidate.sourceId()));
    }

    private WeaponCandidateSelector.Selection select(List<CompiledWeaponCandidate> compiled) {
        List<CompiledWeaponCandidate> merged = new ArrayList<>(lastGood.values());
        compiled.forEach(candidate -> {
            merged.removeIf(existing -> existing.sourceId().equals(candidate.sourceId()));
            merged.add(candidate);
        });
        return selector.select(merged);
    }

    private boolean removeMissingSources(List<ItemSourceCandidate> candidates) {
        Set<String> current = candidates.stream().map(ItemSourceCandidate::sourceId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        boolean removedFingerprints = fingerprints.keySet().removeIf(sourceId -> !current.contains(sourceId));
        boolean removedCandidates = lastGood.keySet().removeIf(sourceId -> !current.contains(sourceId));
        return removedFingerprints || removedCandidates;
    }

    private ItemReloadOutcome publishSelected(ItemCatalogSnapshot previous, List<ItemDefinitionDiagnostic> diagnostics,
                                              ReloadCounters counters) {
        var selection = selector.select(List.copyOf(lastGood.values()));
        List<ItemDefinitionDiagnostic> merged = merge(diagnostics, selection.diagnostics());
        if (merged.stream().anyMatch(ItemDefinitionDiagnostic::error)) {
            return new ItemReloadOutcome(previous, merged, counters, false);
        }
        ItemCatalogSnapshot next = new ItemCatalogSnapshot(previous.revision().next(), map(selection.definitions()));
        return new ItemReloadOutcome(next, merged, counters, true);
    }

    private Map<io.github.kxyr0.echoitem.api.EchoItemId, io.github.kxyr0.echoitem.api.definition.WeaponDefinition> map(
            Collection<io.github.kxyr0.echoitem.api.definition.WeaponDefinition> definitions
    ) {
        Map<io.github.kxyr0.echoitem.api.EchoItemId, io.github.kxyr0.echoitem.api.definition.WeaponDefinition> values =
                new LinkedHashMap<>();
        definitions.forEach(definition -> values.put(definition.id(), definition));
        return values;
    }

    private List<ItemDefinitionDiagnostic> merge(List<ItemDefinitionDiagnostic> first,
                                                 List<ItemDefinitionDiagnostic> second) {
        List<ItemDefinitionDiagnostic> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged;
    }

    private ItemReloadOutcome rejected(ItemCatalogSnapshot previous, ItemCandidateCompiler.CompiledCandidates compiled) {
        return rejected(previous, compiled, List.of());
    }

    private ItemReloadOutcome rejected(ItemCatalogSnapshot previous, ItemCandidateCompiler.CompiledCandidates compiled,
                                       List<ItemDefinitionDiagnostic> extra) {
        List<ItemDefinitionDiagnostic> diagnostics = new ArrayList<>(compiled.diagnostics());
        diagnostics.addAll(extra);
        diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.LAST_KNOWN_GOOD_RETAINED,
                "Reload batch rejected; previous known-good catalog remains active.", "<reload>"));
        return new ItemReloadOutcome(previous, diagnostics, compiled.counters(), false);
    }
}
