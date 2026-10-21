package io.github.kxyr0.echoanimationeditor.api.diagnostic;

import java.util.Objects;

public record AnimatorPublicApiStatus(String boundary, String status, String message) {
    public AnimatorPublicApiStatus {
        boundary = requireText(boundary, "boundary");
        status = requireText(status, "status");
        message = Objects.requireNonNull(message, "message");
    }

    public static AnimatorPublicApiStatus unavailable(String boundary, String message) {
        return new AnimatorPublicApiStatus(boundary, "UNAVAILABLE", message);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
        return value;
    }
}
