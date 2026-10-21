package io.github.kxyr0.echodialogue.api.choice;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable source-authored choice identifier inside one dialogue.
 */
public record ChoiceId(String value) {
    private static final int MAX_LENGTH = 64;
    private static final Pattern PATTERN = Pattern.compile("[a-z][a-z0-9_.-]*");

    public ChoiceId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "ChoiceId должен соответствовать [a-z][a-z0-9_.-]* и быть не длиннее 64 символов: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
