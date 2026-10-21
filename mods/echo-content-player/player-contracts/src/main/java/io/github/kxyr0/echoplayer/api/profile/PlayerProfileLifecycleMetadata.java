package io.github.kxyr0.echoplayer.api.profile;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Minimal durable lifecycle metadata owned by Echo Content Player.
 */
public record PlayerProfileLifecycleMetadata(
        Instant createdAt,
        Instant lastLoginAt,
        Optional<Instant> lastLogoutAt,
        String firstAccountName,
        String lastAccountName,
        boolean defaultTemplateApplied
) {
    public PlayerProfileLifecycleMetadata {
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        lastLoginAt = Objects.requireNonNull(lastLoginAt, "lastLoginAt");
        lastLogoutAt = Objects.requireNonNull(lastLogoutAt, "lastLogoutAt");
        firstAccountName = requireName(firstAccountName, "firstAccountName");
        lastAccountName = requireName(lastAccountName, "lastAccountName");
    }

    public static PlayerProfileLifecycleMetadata firstJoin(Instant now, String accountName) {
        return new PlayerProfileLifecycleMetadata(now, now, Optional.empty(), accountName, accountName, true);
    }

    public PlayerProfileLifecycleMetadata loggedIn(Instant now, String accountName) {
        return new PlayerProfileLifecycleMetadata(createdAt, now, lastLogoutAt, firstAccountName,
                accountName, defaultTemplateApplied);
    }

    public PlayerProfileLifecycleMetadata loggedOut(Instant now) {
        return new PlayerProfileLifecycleMetadata(createdAt, lastLoginAt, Optional.of(now),
                firstAccountName, lastAccountName, defaultTemplateApplied);
    }

    private static String requireName(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return value.strip();
    }
}
