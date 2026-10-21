package io.github.kxyr0.echoplayer.api.identity;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable account-name and Echo display-name snapshot.
 */
public record PlayerIdentitySnapshot(
        EchoPlayerId playerId,
        String accountName,
        PlayerNameMode nameMode,
        Optional<PlayerDisplayName> customDisplayName,
        String resolvedDisplayName
) {
    public PlayerIdentitySnapshot {
        playerId = Objects.requireNonNull(playerId, "playerId");
        accountName = requirePlain(accountName, "accountName");
        nameMode = Objects.requireNonNull(nameMode, "nameMode");
        customDisplayName = Objects.requireNonNull(customDisplayName, "customDisplayName");
        resolvedDisplayName = requirePlain(resolvedDisplayName, "resolvedDisplayName");
        if (nameMode == PlayerNameMode.CUSTOM && customDisplayName.isEmpty()) {
            throw new IllegalArgumentException("CUSTOM name mode requires a custom display name.");
        }
    }

    public static PlayerIdentitySnapshot account(EchoPlayerId playerId, String accountName) {
        return new PlayerIdentitySnapshot(playerId, accountName, PlayerNameMode.ACCOUNT,
                Optional.empty(), accountName);
    }

    public PlayerIdentitySnapshot withAccountName(String nextAccountName) {
        if (nameMode == PlayerNameMode.ACCOUNT) {
            return account(playerId, nextAccountName);
        }
        return new PlayerIdentitySnapshot(playerId, nextAccountName, nameMode,
                customDisplayName, customDisplayName.orElseThrow().value());
    }

    public PlayerIdentitySnapshot withCustomName(PlayerDisplayName displayName) {
        Objects.requireNonNull(displayName, "displayName");
        return new PlayerIdentitySnapshot(playerId, accountName, PlayerNameMode.CUSTOM,
                Optional.of(displayName), displayName.value());
    }

    private static String requirePlain(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value.strip();
    }
}
