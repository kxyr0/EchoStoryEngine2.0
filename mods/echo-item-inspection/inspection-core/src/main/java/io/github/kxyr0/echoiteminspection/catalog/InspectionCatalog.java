package io.github.kxyr0.echoiteminspection.catalog;

import io.github.kxyr0.echoiteminspection.compile.InspectionCompiler;
import io.github.kxyr0.echoiteminspection.compile.InspectionSource;
import io.github.kxyr0.echoiteminspection.diagnostic.InspectionDiagnostic;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinition;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinitionRevision;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Validates a complete candidate batch before one atomic publication; errors preserve the entire LKG. */
public final class InspectionCatalog {
    private final InspectionCompiler compiler = new InspectionCompiler();
    private volatile InspectionCatalogSnapshot current = InspectionCatalogSnapshot.empty();

    public InspectionCatalogSnapshot snapshot() { return current; }

    public Optional<ItemInspectionDefinition> resolve(String itemId) { return current.resolve(itemId); }

    public synchronized InspectionReloadResult reload(List<InspectionSource> sources, Set<String> knownItemIds) {
        ItemInspectionDefinitionRevision revision = new ItemInspectionDefinitionRevision(current.revision().value() + 1);
        List<ItemInspectionDefinition> definitions = new ArrayList<>();
        List<InspectionDiagnostic> diagnostics = new ArrayList<>();
        for (InspectionSource source : sources.stream().sorted(Comparator.comparing(InspectionSource::sourceId)).toList()) {
            var result = compiler.compile(source, revision, knownItemIds);
            diagnostics.addAll(result.diagnostics());
            result.definition().ifPresent(definitions::add);
        }
        validateCollisions(definitions, diagnostics);
        if (diagnostics.stream().anyMatch(InspectionDiagnostic::error)) {
            diagnostics.add(InspectionDiagnostic.error("ITEM_INSPECTION_LAST_KNOWN_GOOD_RETAINED",
                    "Reload rejected; the previous inspection catalog remains active.", diagnostics.get(0).span()));
            return new InspectionReloadResult(false, current, diagnostics);
        }
        current = new InspectionCatalogSnapshot(revision, definitions);
        return new InspectionReloadResult(true, current, diagnostics);
    }

    private void validateCollisions(List<ItemInspectionDefinition> definitions, List<InspectionDiagnostic> diagnostics) {
        Set<String> items = new HashSet<>();
        Set<ItemInspectionId> ids = new HashSet<>();
        for (ItemInspectionDefinition definition : definitions) {
            if (!items.add(definition.itemId())) {
                diagnostics.add(InspectionDiagnostic.error("ITEM_INSPECTION_DUPLICATE_BINDING",
                        "Multiple inspections bind item \"" + definition.itemId() + "\".", definition.descriptor().itemSpan()));
            }
            if (!ids.add(definition.id())) {
                diagnostics.add(InspectionDiagnostic.error("ITEM_INSPECTION_DUPLICATE_RESOURCE",
                        "Duplicate inspection resource ID: " + definition.id(), definition.descriptor().declarationSpan()));
            }
        }
    }
}
