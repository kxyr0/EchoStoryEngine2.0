package io.github.kxyr0.echocutscene.core.binding;

import java.util.Objects;

public record BindingDiagnostic(String code, String message) {
    public BindingDiagnostic {
        code = Objects.requireNonNull(code, "code").trim();
        message = Objects.requireNonNull(message, "message").trim();
        if (code.isEmpty() || message.isEmpty()) {
            throw new IllegalArgumentException("BindingDiagnostic must contain code and message.");
        }
    }
}
