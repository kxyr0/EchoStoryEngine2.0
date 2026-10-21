package io.github.kxyr0.echoplayervitals.api.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable player vitals identity derived from the Minecraft account UUID.
 */
public record PlayerVitalsId(UUID accountUuid) implements Comparable<PlayerVitalsId> {
    public PlayerVitalsId {
        accountUuid = Objects.requireNonNull(accountUuid, "accountUuid");
    }

    public static PlayerVitalsId fromAccountUuid(UUID accountUuid) {
        return new PlayerVitalsId(accountUuid);
    }

    public static PlayerVitalsId parse(String value) {
        Objects.requireNonNull(value, "value");
        return new PlayerVitalsId(UUID.fromString(value));
    }

    public String value() {
        return accountUuid.toString();
    }

    @Override
    public int compareTo(PlayerVitalsId other) {
        return value().compareTo(other.value());
    }

    @Override
    public String toString() {
        return value();
    }
}
