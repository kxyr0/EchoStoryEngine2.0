package io.github.kxyr0.echonpc.core.definition;

import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.service.NpcDefinitionSummary;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable catalog of compiled NPC definitions installed atomically.
 */
public record NpcDefinitionCatalog(
        long revision,
        Map<NpcDefinitionId, NpcDefinition> definitions,
        Map<NpcFormId, NpcFormDefinition> forms
) {
    public NpcDefinitionCatalog {
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must not be negative.");
        }
        definitions = Map.copyOf(Objects.requireNonNull(definitions, "definitions"));
        forms = Map.copyOf(Objects.requireNonNull(forms, "forms"));
    }

    public NpcDefinitionCatalog(long revision, Map<NpcDefinitionId, NpcDefinition> definitions) {
        this(revision, definitions, Map.of());
    }

    public static NpcDefinitionCatalog empty() {
        return new NpcDefinitionCatalog(0L, Map.of());
    }

    public static NpcDefinitionCatalog of(long revision, Collection<NpcDefinition> definitions) {
        return of(revision, definitions, List.of());
    }

    public static NpcDefinitionCatalog of(
            long revision,
            Collection<NpcDefinition> definitions,
            Collection<NpcFormDefinition> forms
    ) {
        Map<NpcDefinitionId, NpcDefinition> indexed = new LinkedHashMap<>();
        for (NpcDefinition definition : definitions) {
            NpcDefinition previous = indexed.putIfAbsent(definition.id(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate NpcDefinitionId: " + definition.id());
            }
        }
        Map<NpcFormId, NpcFormDefinition> indexedForms = new LinkedHashMap<>();
        for (NpcFormDefinition form : forms) {
            NpcFormDefinition previous = indexedForms.putIfAbsent(form.id(), form);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate NpcFormId: " + form.id());
            }
        }
        return new NpcDefinitionCatalog(revision, indexed, indexedForms);
    }

    public Optional<NpcDefinition> find(NpcDefinitionId id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Optional<NpcFormDefinition> findForm(NpcFormId id) {
        return Optional.ofNullable(forms.get(id));
    }

    public List<NpcDefinitionSummary> summaries(int limit) {
        int bounded = Math.max(0, Math.min(limit, 200));
        return definitions.values().stream()
                .map(definition -> new NpcDefinitionSummary(definition.id(), definition.displayName(),
                        definition.nameColor(), definition.skinId(), definition.defaultFormId(),
                        definition.fingerprint()))
                .sorted(Comparator.naturalOrder())
                .limit(bounded)
                .toList();
    }

    public List<NpcFormDefinition> formDefinitions(int limit) {
        int bounded = Math.max(0, Math.min(limit, 200));
        return forms.values().stream().sorted(Comparator.naturalOrder()).limit(bounded).toList();
    }
}
