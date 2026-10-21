package io.github.kxyr0.echoplayer.api.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable Echo player identity derived from the Minecraft account UUID.
 */
public record EchoPlayerId(UUID accountUuid) implements Comparable<EchoPlayerId> {
    public EchoPlayerId {
        accountUuid = Objects.requireNonNull(accountUuid, "accountUuid");
    }

    public static EchoPlayerId fromAccountUuid(UUID accountUuid) {
        return new EchoPlayerId(accountUuid);
    }

    public static EchoPlayerId parse(String value) {
        Objects.requireNonNull(value, "value");
        return new EchoPlayerId(UUID.fromString(value));
    }

    public String value() {
        return accountUuid.toString();
    }

    @Override
    public String toString() {
        return value();
    }

    @Override
    public int compareTo(EchoPlayerId other) {
        return value().compareTo(other.value());
    }
}
