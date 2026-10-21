package io.github.kxyr0.echoiteminspection.model;

import java.util.Objects;

/** Optional authored display text; empty title lets the viewer use the real stack's hover name. */
public record ItemInspectionText(String title, String description) {
    public static final int MAX_TITLE_LENGTH = 256;
    public static final int MAX_DESCRIPTION_LENGTH = 8192;

    public ItemInspectionText {
        validate(title, MAX_TITLE_LENGTH, "Title");
        validate(description, MAX_DESCRIPTION_LENGTH, "Description");
    }

    public static ItemInspectionText defaults() { return new ItemInspectionText("", ""); }

    private static void validate(String text, int maximum, String field) {
        Objects.requireNonNull(text, field);
        if (text.codePointCount(0, text.length()) > maximum) {
            throw new IllegalArgumentException(field + " exceeds " + maximum + " characters.");
        }
    }
}
