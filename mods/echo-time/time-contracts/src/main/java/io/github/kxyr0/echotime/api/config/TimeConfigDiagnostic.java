package io.github.kxyr0.echotime.api.config;

import java.util.Objects;
import java.util.Optional;

public record TimeConfigDiagnostic(String code, String message, String source, Optional<Integer> line) {
    public TimeConfigDiagnostic {
        code = require(code, "code");
        message = require(message, "message");
        source = require(source, "source");
        line = Objects.requireNonNull(line, "line");
    }

    private static String require(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank.");
        return checked;
    }
}
