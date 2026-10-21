package io.github.kxyr0.echotrigger.core.binding;

import io.github.kxyr0.echotrigger.api.model.TriggerDiagnostic;
import io.github.kxyr0.echotrigger.api.status.TriggerDiagnosticCode;

import java.util.List;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;

final class TriggerSemanticDiagnostics {
    private TriggerSemanticDiagnostics() {
    }

    static TriggerDiagnostic diagnostic(TriggerDiagnosticCode code, String message) {
        return new TriggerDiagnostic(code, message);
    }

    static void rejectManualSleepCallbacks(
            List<MethodSyntax> methods,
            List<TriggerDiagnostic> diagnostics) {
        for (MethodSyntax method : methods) {
            boolean manualSleepEvent = method.name().startsWith("OnPlayerSleep")
                    || method.name().startsWith("OnPlayerSeated")
                    || method.name().startsWith("OnPlayerStoodUp")
                    || method.parameters().stream()
                    .anyMatch(parameter -> List.of("PlayerSleepEvent", "PlayerSeatEvent",
                            "PlayerSeatedEvent", "PlayerStoodUpEvent").contains(parameter.type().name().text()));
            if (manualSleepEvent) {
                diagnostics.add(TriggerSemanticDiagnostics.diagnostic(
                        TriggerDiagnosticCode.TRIGGER_STORY_SUBSCRIPTION_INVALID,
                        "Player sleep/seating story routing must use Subscribe(...), not callbacks."));
            }
        }
    }

}
