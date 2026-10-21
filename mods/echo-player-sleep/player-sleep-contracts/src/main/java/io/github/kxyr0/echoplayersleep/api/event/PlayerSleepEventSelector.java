package io.github.kxyr0.echoplayersleep.api.event;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import java.util.Objects;

public record PlayerSleepEventSelector(
        String providerId,
        PlayerSleepEventKind kind,
        PlayerSleepScenarioId scenarioId,
        String fingerprint) {
    public static final String PLAYER_SLEEP_PROVIDER = "echo_player_sleep:events";

    public PlayerSleepEventSelector {
        providerId = requireText(providerId, "providerId");
        kind = Objects.requireNonNull(kind, "kind");
        scenarioId = Objects.requireNonNull(scenarioId, "scenarioId");
        fingerprint = requireText(fingerprint, "fingerprint");
    }

    public static PlayerSleepEventSelector of(PlayerSleepEventKind kind, PlayerSleepScenarioId scenarioId) {
        String key = PLAYER_SLEEP_PROVIDER + "|" + Objects.requireNonNull(kind, "kind").name()
                + "|" + Objects.requireNonNull(scenarioId, "scenarioId").value();
        return new PlayerSleepEventSelector(PLAYER_SLEEP_PROVIDER, kind, scenarioId,
                Integer.toUnsignedString(key.hashCode(), 16));
    }

    public String exactKey() {
        return providerId + "|" + kind.name() + "|" + scenarioId.value();
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
