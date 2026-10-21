package io.github.kxyr0.echocontentdoctor.validation.semantic;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.fact.AliasFact;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AliasValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        Map<ContentResourceId, List<Entry>> aliases = new HashMap<>();
        for (ContentDescriptor descriptor : graph.snapshot().resources()) {
            for (AliasFact alias : descriptor.aliases()) {
                aliases.computeIfAbsent(alias.oldId(), ignored -> new ArrayList<>()).add(new Entry(descriptor, alias));
                if (graph.resolve(descriptor.kind(), alias.target()).isEmpty()) diagnostics.add(error(descriptor, alias,
                        "ECHO_DOCTOR_FORMERLY_TARGET_MISSING", "Formerly target is missing or has the wrong kind."));
            }
        }
        aliases.values().stream().filter(values -> values.stream().map(value -> value.alias.target()).distinct().count() > 1)
                .flatMap(List::stream).forEach(entry -> diagnostics.add(error(entry.descriptor, entry.alias,
                        "ECHO_DOCTOR_FORMERLY_CONFLICT", "Formerly id has multiple canonical targets.")));
        for (List<Entry> entries : aliases.values()) {
            Entry entry = entries.get(0);
            if (cycle(entry.alias.oldId(), aliases, new HashSet<>())) diagnostics.add(error(entry.descriptor,
                    entry.alias, "ECHO_DOCTOR_FORMERLY_CYCLE", "Formerly chain contains a cycle."));
        }
        return diagnostics;
    }

    private boolean cycle(ContentResourceId current, Map<ContentResourceId, List<Entry>> aliases,
            Set<ContentResourceId> visited) {
        if (!visited.add(current)) return true;
        List<Entry> entries = aliases.get(current);
        return entries != null && !entries.isEmpty() && cycle(entries.get(0).alias.target(), aliases, visited);
    }

    private ContentDiagnostic error(ContentDescriptor descriptor, AliasFact alias, String code, String message) {
        return new ContentDiagnostic(ContentSeverity.ERROR, code, descriptor.id(), alias.span(), message, List.of());
    }

    private record Entry(ContentDescriptor descriptor, AliasFact alias) { }
}
