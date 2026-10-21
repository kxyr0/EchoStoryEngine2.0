package io.github.kxyr0.echocontentdoctor.validation.semantic;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.fact.TriggerFact;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.List;

public final class ObservationTriggerValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        for (ContentDescriptor descriptor : graph.snapshot().resources()) {
            if (descriptor.kind() == ContentResourceKind.OBSERVATION && descriptor.enabled() && !descriptor.placed()) {
                diagnostics.add(new ContentDiagnostic(ContentSeverity.ERROR, "ECHO_DOCTOR_OBSERVATION_UNPLACED",
                        descriptor.id(), descriptor.span(), "Enabled observation has no At placement.", List.of()));
            }
            for (TriggerFact trigger : descriptor.triggers()) {
                if (!trigger.providerExists() || !trigger.selectorValid()) diagnostics.add(new ContentDiagnostic(
                        ContentSeverity.ERROR, "ECHO_DOCTOR_TRIGGER_INVALID", descriptor.id(), trigger.span(),
                        "Trigger provider or selector is invalid.", List.of()));
                else if (trigger.constantFalse()) diagnostics.add(new ContentDiagnostic(ContentSeverity.ERROR,
                        "ECHO_DOCTOR_TRIGGER_IMPOSSIBLE", descriptor.id(), trigger.span(),
                        "Trigger condition is provably false.", List.of()));
            }
        }
        return diagnostics;
    }
}
