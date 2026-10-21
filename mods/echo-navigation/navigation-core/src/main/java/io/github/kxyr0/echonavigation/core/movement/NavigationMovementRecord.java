package io.github.kxyr0.echonavigation.core.movement;

import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.api.movement.NavigationProgressSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import java.util.Optional;

final class NavigationMovementRecord {
    private final NavigationRequest request;
    private NavigationStatus status = NavigationStatus.CREATED;
    private Optional<NavigationOutcome> outcome = Optional.empty();
    private Optional<NavigationFailure> failure = Optional.empty();
    private NavigationProgressSnapshot progress;

    NavigationMovementRecord(NavigationRequest request, Vector3 currentPosition, long tick) {
        this.request = request;
        double distance = request.destination().standingPosition().horizontalDistanceTo(currentPosition);
        this.progress = NavigationProgressSnapshot.initial(currentPosition, distance, tick);
    }

    NavigationRequest request() {
        return request;
    }

    NavigationStatus status() {
        return status;
    }

    NavigationProgressSnapshot progress() {
        return progress;
    }

    void status(NavigationStatus next) {
        if (!status.terminal()) {
            status = next;
        }
    }

    void progress(NavigationProgressSnapshot next) {
        progress = next;
    }

    void replan() {
        progress = new NavigationProgressSnapshot(progress.previousPosition(), progress.currentPosition(),
                progress.remainingDistance(), progress.bestDistance(), progress.lastProgressTick(),
                progress.serverTick(), progress.pathIndex(), progress.repathCount() + 1, progress.movementTicks());
        status(NavigationStatus.REPLANNING);
    }

    void arrive() {
        status = NavigationStatus.ARRIVED;
        outcome = Optional.of(NavigationOutcome.ARRIVED);
    }

    void cancel(NavigationOutcome value) {
        status = NavigationStatus.CANCELLED;
        outcome = Optional.of(value);
        failure = Optional.of(NavigationFailure.NPC_MOVE_CANCELLED);
    }

    void fail(NavigationStatus terminal, NavigationOutcome value, NavigationFailure code) {
        status = terminal;
        outcome = Optional.of(value);
        failure = Optional.of(code);
    }

    NavigationSnapshot snapshot() {
        return new NavigationSnapshot(request.moveId(), request.commandId(), request.executionId(),
                request.ownerId(), request.subjectId(), request.destination(), request.destination().standingPosition(),
                status, outcome, failure, request.backendId(), request.settingsRevision(), progress, request.gait(),
                request.speedBlocksPerSecond(), request.synchronizePresentationSpeed());
    }
}
