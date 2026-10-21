package io.github.kxyr0.echotime.api.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public record TimeOccurrenceId(String value) {
    public TimeOccurrenceId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Time occurrence ID cannot be blank.");
    }

    public static TimeOccurrenceId derive(String selectorId, long day, long targetMicros, String generation) {
        String source = selectorId + "|" + day + "|" + targetMicros + "|" + generation;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return new TimeOccurrenceId("time-occurrence:" + HexFormat.of().formatHex(digest, 0, 16));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
