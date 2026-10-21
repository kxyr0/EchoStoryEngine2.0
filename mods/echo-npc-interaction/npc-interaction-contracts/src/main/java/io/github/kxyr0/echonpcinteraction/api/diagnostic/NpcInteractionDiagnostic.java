package io.github.kxyr0.echonpcinteraction.api.diagnostic;

import java.util.Map;
import java.util.Objects;

public record NpcInteractionDiagnostic(
        String code,
        String source,
        String messageRu,
        String severity,
        long serverTick,
        Map<String, String> context) {
    public NpcInteractionDiagnostic {
        code = text(code, "code");
        source = text(source, "source");
        messageRu = text(messageRu, "messageRu");
        severity = text(severity, "severity");
        context = Map.copyOf(Objects.requireNonNull(context, "context"));
        if (serverTick < 0L) {
            throw new IllegalArgumentException("serverTick must not be negative.");
        }
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must be non-empty.");
        }
        return checked;
    }
}
