package io.github.kxyr0.echoplayersleep.script;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepDreamDestination;
import io.github.kxyr0.echoplayersleep.api.world.DimensionId;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import io.github.kxyr0.echoplayersleep.api.world.Vector3;
import io.github.kxyr0.echoplayersleep.forge.api.context.PlayerSleepStoryContextBridge;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record SleepPayload(Map<String, String> values) {
    public SleepPayload {
        values = Map.copyOf(values);
    }

    public static SleepPayload inherited(String storyExecutionId, Map<String, String> explicitValues) {
        return new SleepPayload(PlayerSleepStoryContextBridge.merge(storyExecutionId, explicitValues));
    }

    public PlayerRef player() {
        return new PlayerRef(UUID.fromString(required("player", "sleep.player")));
    }

    public PlayerSleepScenarioId scenarioId() {
        return new PlayerSleepScenarioId(required("scenario", "sleep.scenario"));
    }

    public PlayerSleepOwnerId owner(String fallback) {
        return new PlayerSleepOwnerId(optional("owner", "sleep.owner", fallback));
    }

    public PlayerSleepCommandId command(String fallback) {
        return new PlayerSleepCommandId(optional("command", "sleep.command", fallback));
    }

    public Ticks duration(long fallback) {
        return Ticks.of(Long.parseLong(optional("duration", "sleep.duration", Long.toString(fallback))));
    }

    public long waitTicks(long fallback) {
        String seconds = optional("seconds", "sleep.seconds", "");
        if (!seconds.isBlank()) {
            return Math.max(1L, Math.round(Double.parseDouble(seconds) * 20.0D));
        }
        String ticks = optional("ticks", "sleep.ticks", "");
        if (!ticks.isBlank()) {
            return Math.max(1L, Long.parseLong(ticks));
        }
        return Math.max(1L, duration(fallback).value());
    }

    public boolean sleepyBlink() {
        return Boolean.parseBoolean(values.getOrDefault("sleepyBlink", "false"));
    }

    public boolean showHud() {
        return Boolean.parseBoolean(optional("showHud", "sleep.showHud", "true"));
    }

    public Optional<String> blackspaceInstanceId() {
        String value = optional("blackspaceInstanceId", "sleep.blackspaceInstanceId", "");
        if (value.isBlank()) {
            value = optional("blackspaceId", "sleep.blackspaceId", "");
        }
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public PlayerSleepDreamDestination destination() {
        String target = optional("target", "sleep.target", "");
        if (!target.isBlank()) {
            return packedDestination(target);
        }
        return new PlayerSleepDreamDestination(new DimensionId(required("dimension", "sleep.dimension")),
                new Vector3(number("x"), number("y"), number("z")),
                (float) number("yaw"), (float) number("pitch"));
    }

    private PlayerSleepDreamDestination packedDestination(String target) {
        String[] parts = target.split(";");
        if (parts.length != 6) {
            throw new IllegalArgumentException("PLAYER_SLEEP_DESTINATION_INVALID: target");
        }
        return new PlayerSleepDreamDestination(new DimensionId(parts[0]),
                new Vector3(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3])),
                Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
    }

    private double number(String name) {
        return Double.parseDouble(required(name, "sleep." + name));
    }

    private String optional(String primary, String secondary, String fallback) {
        String value = values.get(primary);
        if (value == null || value.isBlank()) {
            value = values.get(secondary);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    private String required(String primary, String secondary) {
        String value = optional(primary, secondary, "");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PLAYER_SLEEP_CONTEXT_MISSING: " + primary);
        }
        return value;
    }
}
