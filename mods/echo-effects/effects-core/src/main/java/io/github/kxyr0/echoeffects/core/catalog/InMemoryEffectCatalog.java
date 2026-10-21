package io.github.kxyr0.echoeffects.core.catalog;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticCode;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectResult;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryEffectCatalog {
    private Map<EffectId, EffectDefinition> accepted = Map.of();

    public EffectResult<EffectCatalogSnapshot> publish(List<EffectDefinition> definitions) {
        Map<EffectId, EffectDefinition> next = new LinkedHashMap<>();
        for (EffectDefinition definition : definitions) {
            if (next.putIfAbsent(definition.id(), definition) != null) {
                return EffectResult.invalid(List.of(EffectDiagnostic.error(
                        EffectDiagnosticCode.EFFECT_DUPLICATE_ID,
                        "Duplicate EffectId: " + definition.id().value(),
                        definition.id().value())));
            }
        }
        accepted = Map.copyOf(next);
        return EffectResult.valid(snapshot(), List.of());
    }

    public Optional<EffectDefinition> find(EffectId id) {
        return Optional.ofNullable(accepted.get(id));
    }

    public EffectCatalogSnapshot snapshot() {
        return new EffectCatalogSnapshot(accepted.values().stream().toList());
    }
}
