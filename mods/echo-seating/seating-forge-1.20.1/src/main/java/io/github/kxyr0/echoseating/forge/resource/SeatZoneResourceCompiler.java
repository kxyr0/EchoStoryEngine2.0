package io.github.kxyr0.echoseating.forge.resource;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.core.binding.SeatZoneDefinitionBinder;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.ArrayList;
import java.util.List;

public final class SeatZoneResourceCompiler {
    private final SeatZoneDefinitionBinder binder = new SeatZoneDefinitionBinder();

    public Result compile(List<SeatZoneResourceText> resources) {
        List<SeatZoneDefinition> definitions = new ArrayList<>();
        List<SeatingDiagnostic> diagnostics = new ArrayList<>();
        for (SeatZoneResourceText resource : resources) {
            var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                    new SourceId(resource.sourceId()), resource.text()));
            if (!parsed.successful()) {
                diagnostics.add(SeatingDiagnostic.error(SeatingDiagnosticCode.SEATING_ZONE_INVALID,
                        resource.sourceId(), parsed.diagnostics().toString()));
                continue;
            }
            var result = binder.bind(parsed.syntaxTree().orElseThrow(), SeatingRevision.initial());
            result.definition().ifPresent(definitions::add);
            diagnostics.addAll(result.diagnostics());
        }
        return new Result(List.copyOf(definitions), List.copyOf(diagnostics));
    }

    public record Result(List<SeatZoneDefinition> definitions, List<SeatingDiagnostic> diagnostics) {
        public boolean successful() {
            return diagnostics.isEmpty();
        }
    }
}
