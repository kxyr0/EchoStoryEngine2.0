package io.github.kxyr0.echoitem.core.reload;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoitem.core.source.ItemSourceOrigin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects active definitions by source precedence and detects same-origin duplicates.
 */
final class WeaponCandidateSelector {
    Selection select(List<CompiledWeaponCandidate> candidates) {
        Map<EchoItemId, List<CompiledWeaponCandidate>> byId = new LinkedHashMap<>();
        candidates.forEach(candidate -> byId.computeIfAbsent(candidate.definition().id(), ignored -> new ArrayList<>())
                .add(candidate));
        List<WeaponDefinition> selected = new ArrayList<>();
        List<ItemDefinitionDiagnostic> diagnostics = new ArrayList<>();
        byId.forEach((id, matches) -> selectOne(id, matches, selected, diagnostics));
        return new Selection(selected, diagnostics);
    }

    private void selectOne(EchoItemId id, List<CompiledWeaponCandidate> matches,
                           List<WeaponDefinition> selected, List<ItemDefinitionDiagnostic> diagnostics) {
        if (hasSameOriginDuplicate(matches, ItemSourceOrigin.EXTERNAL)
                || hasSameOriginDuplicate(matches, ItemSourceOrigin.PACKAGED)) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.DUPLICATE_ITEM_ID,
                    "Duplicate item ID: " + id, matches.get(0).sourceId()));
            return;
        }
        selected.add(matches.stream()
                .sorted(Comparator.comparingInt(this::precedence))
                .findFirst()
                .orElseThrow()
                .definition());
    }

    private boolean hasSameOriginDuplicate(List<CompiledWeaponCandidate> matches, ItemSourceOrigin origin) {
        return matches.stream().filter(candidate -> candidate.origin() == origin).count() > 1L;
    }

    private int precedence(CompiledWeaponCandidate candidate) {
        return candidate.origin() == ItemSourceOrigin.EXTERNAL ? 0 : 1;
    }

    record Selection(List<WeaponDefinition> definitions, List<ItemDefinitionDiagnostic> diagnostics) {
        Selection {
            definitions = List.copyOf(definitions);
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
