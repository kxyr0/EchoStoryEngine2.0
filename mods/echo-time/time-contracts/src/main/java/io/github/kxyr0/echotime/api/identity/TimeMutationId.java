package io.github.kxyr0.echotime.api.identity;

import java.util.Objects;

public record TimeMutationId(String value) {
    public TimeMutationId {
        value = require(value, "Time mutation ID");
    }

    private static String require(String value, String label) {
        String checked = Objects.requireNonNull(value, "value").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(label + " cannot be blank.");
        return checked;
    }
}
