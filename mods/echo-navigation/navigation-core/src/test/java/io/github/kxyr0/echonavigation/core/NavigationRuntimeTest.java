package io.github.kxyr0.echonavigation.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import io.github.kxyr0.echonavigation.api.movement.NavigationOutcome;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.position.Vector3;
import io.github.kxyr0.echonavigation.core.movement.MovementStartResult;
import io.github.kxyr0.echonavigation.core.movement.NavigationRuntime;
import org.junit.jupiter.api.Test;

final class NavigationRuntimeTest {
    private final NavigationRuntime runtime = new NavigationRuntime();
    private final NavigationSettingsSnapshot settings = NavigationSettingsSnapshot.defaults();

    @Test
    void duplicateCommandReturnsSameMove() {
        NavigationRequest request = request("move_a", "cmd_a", "npc_a");
        assertEquals(MovementStartResult.Status.ACCEPTED, start(request).status());
        MovementStartResult duplicate = start(request);
        assertEquals(MovementStartResult.Status.DUPLICATE_COMMAND, duplicate.status());
        assertEquals(request.moveId(), duplicate.snapshot().orElseThrow().moveId());
    }

    @Test
    void oneActiveMovePerSubjectRejectsNewMove() {
        assertEquals(MovementStartResult.Status.ACCEPTED, start(request("move_a", "cmd_a", "npc_a")).status());
        assertEquals(MovementStartResult.Status.SUBJECT_BUSY, start(request("move_b", "cmd_b", "npc_a")).status());
    }

    @Test
    void cancellationIsTerminalAndClearsSubjectSlot() {
        NavigationRequest first = request("move_a", "cmd_a", "npc_a");
        start(first);
        runtime.cancel(first.moveId(), NavigationOutcome.CANCELLED);
        assertEquals(NavigationStatus.CANCELLED, runtime.query(first.moveId()).orElseThrow().status());
        assertEquals(MovementStartResult.Status.ACCEPTED, start(request("move_b", "cmd_b", "npc_a")).status());
    }

    @Test
    void arrivedClearsMovingPredicate() {
        NavigationRequest request = request("move_a", "cmd_a", "npc_a");
        start(request);
        assertTrue(runtime.isMoving(request.subjectId()));
        runtime.arrived(request.moveId());
        assertEquals(NavigationStatus.ARRIVED, runtime.query(request.moveId()).orElseThrow().status());
    }

    @Test
    void snapshotPreservesRequestedGaitSpeedAndSyncPolicy() {
        NavigationRequest base = request("move_sprint", "cmd_sprint", "npc_a");
        NavigationRequest request = new NavigationRequest(base.moveId(), base.commandId(), base.executionId(),
                base.ownerId(), base.subjectId(), base.destination(), base.backendId(), base.settingsRevision(),
                base.commandGeneration(), NavigationGait.SPRINT, 5.5D, false);

        var snapshot = start(request).snapshot().orElseThrow();

        assertEquals(NavigationGait.SPRINT, snapshot.gait());
        assertEquals(5.5D, snapshot.speedBlocksPerSecond());
        assertEquals(false, snapshot.synchronizePresentationSpeed());
    }

    private MovementStartResult start(NavigationRequest request) {
        return runtime.start(request, new Vector3(0.5D, 64.0D, 0.5D), settings, 1L);
    }

    private NavigationRequest request(String move, String command, String subject) {
        return new NavigationRequest(new NavigationMoveId("test:" + move), new NavigationCommandId("test:" + command),
                new NavigationExecutionId("test:execution"), new NavigationOwnerId("test:owner"),
                new NavigationSubjectId("jdh:" + subject), new NavigationDestination("minecraft:overworld", 10, 64, 10),
                NavigationBackendId.VANILLA, settings.revision(), 0L);
    }
}
