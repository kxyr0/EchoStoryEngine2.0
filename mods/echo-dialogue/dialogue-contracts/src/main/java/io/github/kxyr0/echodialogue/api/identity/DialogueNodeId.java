package io.github.kxyr0.echodialogue.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable node identifier inside one dialogue.
 */
public record DialogueNodeId(String value) {
    private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9_.:-]+");

    public DialogueNodeId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("ID узла диалога пустой или содержит недопустимые символы: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
