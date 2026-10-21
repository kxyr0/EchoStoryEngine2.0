package io.github.kxyr0.echovisibility.core.arm;

import io.github.kxyr0.echovisibility.api.identity.VisibilityPlayerRef;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.api.report.VisibilityTransition;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityWatchStatus;
import java.util.Objects;
import java.util.Optional;

public final class ArmedVisibilityWatch {
    private final VisibilityPlayerRef player;
    private final VisibilityWatchDefinition definition;
    private final VisibilityWatchGeneration generation;
    private final String ownerLineage;
    private final long createdTick;
    private long connectionGeneration;
    private VisibilityWatchStatus status;
    private VisibilityState lastState = VisibilityState.UNKNOWN;
    private Optional<VisibilityTransition> lastTransition = Optional.empty();
    private long lastAcceptedSequence;
    private long lastAcceptedClientTick = -1L;
    private long lastAcceptedServerTick = -1L;

    public ArmedVisibilityWatch(
            VisibilityPlayerRef player,
            VisibilityWatchDefinition definition,
            VisibilityWatchGeneration generation,
            String ownerLineage,
            long createdTick,
            long connectionGeneration) {
        this.player = Objects.requireNonNull(player, "player");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.ownerLineage = requireLineage(ownerLineage);
        if (createdTick < 0L || connectionGeneration <= 0L) {
            throw new IllegalArgumentException("Arm tick and connection generation are invalid.");
        }
        this.createdTick = createdTick;
        this.connectionGeneration = connectionGeneration;
        this.status = definition.requireVisibleFirst()
                ? VisibilityWatchStatus.ARMED_WAITING_FOR_VISIBLE
                : VisibilityWatchStatus.LOST_SIGHT;
    }

    public void accept(VisibilityReport report, long serverTick) {
        lastState = report.state();
        lastTransition = Optional.of(report.transition());
        lastAcceptedSequence = report.reportSequence();
        lastAcceptedClientTick = report.clientTick();
        lastAcceptedServerTick = serverTick;
        if (lastState.visible()) {
            status = VisibilityWatchStatus.TRACKING_VISIBLE;
        } else if (lastState.geometricallyHidden()) {
            status = VisibilityWatchStatus.LOST_SIGHT;
        } else if (lastState == VisibilityState.TARGET_UNAVAILABLE) {
            status = VisibilityWatchStatus.TARGET_UNAVAILABLE;
        }
    }

    public void reconnect(long newConnectionGeneration) {
        if (newConnectionGeneration <= connectionGeneration) {
            throw new IllegalArgumentException("Connection generation must advance on reconnect.");
        }
        connectionGeneration = newConnectionGeneration;
        status = definition.requireVisibleFirst()
                ? VisibilityWatchStatus.ARMED_WAITING_FOR_VISIBLE
                : VisibilityWatchStatus.LOST_SIGHT;
        lastState = VisibilityState.UNKNOWN;
        lastTransition = Optional.empty();
        lastAcceptedSequence = 0L;
        lastAcceptedClientTick = -1L;
        lastAcceptedServerTick = -1L;
    }

    public VisibilityArmSnapshot snapshot() {
        return new VisibilityArmSnapshot(player, definition, generation, ownerLineage, createdTick,
                connectionGeneration, status, lastState, lastTransition, lastAcceptedSequence,
                lastAcceptedClientTick, lastAcceptedServerTick);
    }

    public VisibilityWatchDefinition definition() {
        return definition;
    }

    public VisibilityWatchGeneration generation() {
        return generation;
    }

    public long connectionGeneration() {
        return connectionGeneration;
    }

    public long lastAcceptedSequence() {
        return lastAcceptedSequence;
    }

    public long lastAcceptedClientTick() {
        return lastAcceptedClientTick;
    }

    private static String requireLineage(String value) {
        String checked = Objects.requireNonNull(value, "ownerLineage").trim();
        if (checked.isEmpty() || checked.length() > 192) {
            throw new IllegalArgumentException("Owner lineage must contain 1..192 characters.");
        }
        return checked;
    }
}
