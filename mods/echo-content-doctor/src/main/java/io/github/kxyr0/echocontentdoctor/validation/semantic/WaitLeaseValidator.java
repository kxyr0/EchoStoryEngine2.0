package io.github.kxyr0.echocontentdoctor.validation.semantic;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.fact.LeaseFact;
import io.github.kxyr0.echocontentdoctor.fact.WaitFact;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.List;

public final class WaitLeaseValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        for (ContentDescriptor descriptor : graph.snapshot().resources()) {
            for (WaitFact wait : descriptor.waits()) {
                if (wait.clockPaused() && !wait.resumeReachable()) diagnostics.add(error(descriptor, wait,
                        "ECHO_DOCTOR_WAIT_IMPOSSIBLE", "Wait cannot complete while time is paused."));
                if (wait.heldCapability().isPresent() && wait.heldCapability().equals(wait.childRequiredCapability())) {
                    diagnostics.add(error(descriptor, wait, "ECHO_DOCTOR_LEASE_DEADLOCK",
                            "Parent waits for a child requiring its exclusive capability."));
                }
            }
            for (LeaseFact lease : descriptor.leases()) {
                if (!lease.automatic() && !lease.releasedOnAllTerminalPaths()) diagnostics.add(new ContentDiagnostic(
                        ContentSeverity.ERROR, "ECHO_DOCTOR_LEASE_LEAK", descriptor.id(), lease.span(),
                        "Manual lease " + lease.capability() + " has a terminal path without release.", List.of()));
            }
        }
        return diagnostics;
    }

    private ContentDiagnostic error(ContentDescriptor descriptor, WaitFact wait, String code, String message) {
        return new ContentDiagnostic(ContentSeverity.ERROR, code, descriptor.id(), wait.span(), message, List.of());
    }
}
