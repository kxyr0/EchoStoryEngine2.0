package io.github.kxyr0.echoplayer.api.mutation;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Durable idempotency key for profile mutation requests.
 */
public record PlayerMutationId(String value) implements Comparable<PlayerMutationId> {
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_.:-]{1,96}");

    public PlayerMutationId {
        value = Objects.requireNonNull(value, "value").strip();
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException("Mutation ID must be 1-96 safe identifier characters.");
        }
    }

    public static PlayerMutationId random() {
        return new PlayerMutationId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(PlayerMutationId other) {
        return value.compareTo(other.value);
    }
}
