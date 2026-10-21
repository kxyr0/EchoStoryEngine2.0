package io.github.kxyr0.echoobservation.forge.resource;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnostic.Severity;
import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import io.github.kxyr0.echoobservation.api.model.ObservationResourceDefinition;
import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalogCandidate;
import io.github.kxyr0.echoobservation.echoscript.ObservationResourceCompiler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ObservationCandidateCompiler {
    private final ObservationResourceCompiler compiler = new ObservationResourceCompiler();

    ObservationCatalogCandidate compile(List<ObservationResourceText> sources) {
        List<ObservationResourceDefinition> resources = new ArrayList<>();
        List<ObservationDiagnostic> diagnostics = new ArrayList<>();
        sources.forEach(source -> {
            var result = compiler.compile(source.sourceId(), source.source());
            diagnostics.addAll(result.diagnostics());
            result.resource().ifPresent(resources::add);
        });
        Set<Object> ids = new HashSet<>();
        resources.stream().filter(value -> !ids.add(value.id())).forEach(value -> diagnostics.add(
                new ObservationDiagnostic(ObservationDiagnosticCode.OBSERVATION_ID_COLLISION, Severity.ERROR,
                        "Duplicate Observation resource: " + value.id(), 0)));
        return new ObservationCatalogCandidate(resources, diagnostics);
    }
}
