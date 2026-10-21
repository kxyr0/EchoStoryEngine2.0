package io.github.kxyr0.echonpc.persistence.schema;

import java.util.Objects;

public record FormSaveRecord(String formId, long revision, String fingerprint) {
    public FormSaveRecord {
        formId = text(formId, "formId");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must not be negative.");
        }
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint").trim().toLowerCase(java.util.Locale.ROOT);
        if (!fingerprint.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("fingerprint must be SHA-256 hex.");
        }
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 256) {
            throw new IllegalArgumentException(name + " must be non-empty and bounded.");
        }
        return checked;
    }
}
