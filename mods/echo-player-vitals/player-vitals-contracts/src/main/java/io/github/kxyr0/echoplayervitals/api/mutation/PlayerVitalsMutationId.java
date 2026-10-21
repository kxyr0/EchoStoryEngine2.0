package io.github.kxyr0.echoplayervitals.api.mutation;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Durable idempotency key for vitals mutation requests.
 */
public record PlayerVitalsMutationId(String value) implements Comparable<PlayerVitalsMutationId> {
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_.:-]{1,96}");

    public PlayerVitalsMutationId {
        value = Objects.requireNonNull(value, "value").strip();
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException("Vitals mutation ID must be 1-96 safe identifier characters.");
        }
    }

    public static PlayerVitalsMutationId random() {
        return new PlayerVitalsMutationId(UUID.randomUUID().toString());
    }

    @Override
    public int compareTo(PlayerVitalsMutationId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
