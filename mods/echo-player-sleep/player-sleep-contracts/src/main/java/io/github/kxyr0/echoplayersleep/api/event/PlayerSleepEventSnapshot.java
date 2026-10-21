package io.github.kxyr0.echoplayersleep.api.event;

import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.BlockId;
import io.github.kxyr0.echoplayersleep.api.world.BlockPosition;
import io.github.kxyr0.echoplayersleep.api.world.DimensionId;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import java.util.Objects;

public record PlayerSleepEventSnapshot(
        PlayerSleepEventId eventId,
        PlayerSleepSessionRef session,
        PlayerSleepEventKind kind,
        PlayerSleepScenarioId scenarioId,
        PlayerRef player,
        DimensionId originDimension,
        BlockPosition bedPosition,
        BlockId bedBlock,
        String bedProviderId,
        long sleepCount,
        long serverTick,
        String fingerprint) {
    public PlayerSleepEventSnapshot {
        eventId = Objects.requireNonNull(eventId, "eventId");
        session = Objects.requireNonNull(session, "session");
        kind = Objects.requireNonNull(kind, "kind");
        scenarioId = Objects.requireNonNull(scenarioId, "scenarioId");
        player = Objects.requireNonNull(player, "player");
        originDimension = Objects.requireNonNull(originDimension, "originDimension");
        bedPosition = Objects.requireNonNull(bedPosition, "bedPosition");
        bedBlock = Objects.requireNonNull(bedBlock, "bedBlock");
        bedProviderId = requireText(bedProviderId, "bedProviderId");
        if (sleepCount <= 0L) {
            throw new IllegalArgumentException("sleepCount must be positive.");
        }
        fingerprint = requireText(fingerprint, "fingerprint");
    }

    public static PlayerSleepEventSnapshot from(
            PlayerSleepEventKind kind,
            PlayerSleepSessionSnapshot snapshot,
            long serverTick) {
        PlayerSleepEventSelector selector = PlayerSleepEventSelector.of(kind, snapshot.scenarioId());
        String eventKey = snapshot.sessionId().value() + "|" + snapshot.revision().value() + "|" + kind.name();
        return new PlayerSleepEventSnapshot(
                new PlayerSleepEventId("sleep-event:" + Integer.toUnsignedString(eventKey.hashCode(), 16)),
                new PlayerSleepSessionRef(snapshot.sessionId(), snapshot.revision()),
                kind,
                snapshot.scenarioId(),
                snapshot.player(),
                snapshot.origin().dimension(),
                snapshot.bed().canonicalPosition(),
                snapshot.bed().actualBlock(),
                snapshot.bed().providerId(),
                snapshot.sleepCount(),
                serverTick,
                selector.fingerprint());
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
