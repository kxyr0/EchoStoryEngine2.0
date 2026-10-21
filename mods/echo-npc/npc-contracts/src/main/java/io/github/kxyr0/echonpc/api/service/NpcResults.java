package io.github.kxyr0.echonpc.api.service;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.rest.NpcRestAttachmentView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit immutable operation results for the neutral NpcService.
 */
public final class NpcResults {
    private NpcResults() {
    }

    public enum SpawnStatus {
        QUEUED, REJECTED, CREATED, ALREADY_ACTIVE, ACTOR_ID_CONFLICT, DEFINITION_NOT_FOUND,
        WORLD_NOT_FOUND, INVALID_REQUEST, PLATFORM_REJECTED, SERVICE_STOPPED, FAILURE
    }

    public enum QueryStatus {
        FOUND, NOT_FOUND, SERVICE_STOPPED, FAILURE
    }

    public enum TeleportStatus {
        QUEUED, REJECTED, MOVED, NOT_FOUND, WORLD_NOT_FOUND, INVALID_REQUEST, PLATFORM_REJECTED, SERVICE_STOPPED,
        FAILURE
    }

    public enum DespawnStatus {
        QUEUED, REJECTED, TOMBSTONED, REMOVED, ALREADY_ABSENT, PLATFORM_REJECTED, SERVICE_STOPPED, FAILURE
    }

    public enum SleepStatus {
        QUEUED, REJECTED, SLEEPING, ALREADY_SLEEPING, NOT_FOUND, NOT_MATERIALIZED, TOO_FAR, BED_NOT_FOUND,
        EXPECTED_BLOCK_MISMATCH, BED_UNSUPPORTED, BED_OCCUPIED, PROVIDER_NOT_FOUND, ATTACHMENT_TIMEOUT,
        SERVICE_STOPPED, FAILURE
    }

    public enum WakeStatus {
        QUEUED, REJECTED, WOKE_UP, ALREADY_AWAKE, NOT_FOUND, POSITION_INVALID, SERVICE_STOPPED, FAILURE
    }

    public enum OperationKind {
        SPAWN,
        TELEPORT,
        DESPAWN,
        SLEEP,
        WAKE,
        FORM_CHANGE,
        SKIN_CHANGE,
        PLACE,
        RECOVERY_RETRY
    }

    public enum OperationStatus {
        QUEUED,
        RUNNING,
        SUCCEEDED,
        FAILED,
        REJECTED
    }

    public enum OperationQueryStatus {
        FOUND,
        NOT_FOUND
    }

    public record Spawn(
            SpawnStatus status,
            Optional<NpcActorView> actor,
            Optional<String> operationId,
            String message
    ) {
        public Spawn {
            status = Objects.requireNonNull(status, "status");
            actor = Objects.requireNonNull(actor, "actor");
            operationId = Objects.requireNonNull(operationId, "operationId");
            message = Objects.requireNonNull(message, "message");
        }

        public Spawn(SpawnStatus status, Optional<NpcActorView> actor, String message) {
            this(status, actor, Optional.empty(), message);
        }
    }

    public record Query(QueryStatus status, Optional<NpcActorView> actor, String message) {
        public Query {
            status = Objects.requireNonNull(status, "status");
            actor = Objects.requireNonNull(actor, "actor");
            message = Objects.requireNonNull(message, "message");
        }
    }

    public record Teleport(
            TeleportStatus status,
            Optional<NpcActorView> actor,
            Optional<String> operationId,
            String message
    ) {
        public Teleport {
            status = Objects.requireNonNull(status, "status");
            actor = Objects.requireNonNull(actor, "actor");
            operationId = Objects.requireNonNull(operationId, "operationId");
            message = Objects.requireNonNull(message, "message");
        }

        public Teleport(TeleportStatus status, Optional<NpcActorView> actor, String message) {
            this(status, actor, Optional.empty(), message);
        }
    }

    public record Despawn(DespawnStatus status, Optional<String> operationId, String message) {
        public Despawn {
            status = Objects.requireNonNull(status, "status");
            operationId = Objects.requireNonNull(operationId, "operationId");
            message = Objects.requireNonNull(message, "message");
        }

        public Despawn(DespawnStatus status, String message) {
            this(status, Optional.empty(), message);
        }
    }

    public record Sleep(
            SleepStatus status,
            Optional<NpcActorView> actor,
            Optional<String> operationId,
            Optional<NpcRestAttachmentView> attachment,
            String message
    ) {
        public Sleep {
            status = Objects.requireNonNull(status, "status");
            actor = Objects.requireNonNull(actor, "actor");
            operationId = Objects.requireNonNull(operationId, "operationId");
            attachment = Objects.requireNonNull(attachment, "attachment");
            message = Objects.requireNonNull(message, "message");
        }

        public Sleep(SleepStatus status, Optional<NpcActorView> actor, String message) {
            this(status, actor, Optional.empty(), Optional.empty(), message);
        }
    }

    public record Wake(
            WakeStatus status,
            Optional<NpcActorView> actor,
            Optional<String> operationId,
            String message
    ) {
        public Wake {
            status = Objects.requireNonNull(status, "status");
            actor = Objects.requireNonNull(actor, "actor");
            operationId = Objects.requireNonNull(operationId, "operationId");
            message = Objects.requireNonNull(message, "message");
        }

        public Wake(WakeStatus status, Optional<NpcActorView> actor, String message) {
            this(status, actor, Optional.empty(), message);
        }
    }

    public record ListActors(List<NpcActorView> actors, boolean truncated) {
        public ListActors {
            actors = List.copyOf(Objects.requireNonNull(actors, "actors"));
        }
    }

    public record Operation(
            String operationId,
            OperationKind kind,
            OperationStatus status,
            ActorId actorId,
            Optional<NpcActorView> actor,
            String message
    ) {
        public Operation {
            operationId = requireId(operationId);
            kind = Objects.requireNonNull(kind, "kind");
            status = Objects.requireNonNull(status, "status");
            actorId = Objects.requireNonNull(actorId, "actorId");
            actor = Objects.requireNonNull(actor, "actor");
            message = Objects.requireNonNull(message, "message");
        }
    }

    public record QueryOperation(OperationQueryStatus status, Optional<Operation> operation, String message) {
        public QueryOperation {
            status = Objects.requireNonNull(status, "status");
            operation = Objects.requireNonNull(operation, "operation");
            message = Objects.requireNonNull(message, "message");
        }
    }

    public record ListOperations(List<Operation> operations, boolean truncated) {
        public ListOperations {
            operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
        }
    }

    private static String requireId(String value) {
        String checked = Objects.requireNonNull(value, "operationId").trim();
        if (checked.isEmpty() || checked.length() > 64) {
            throw new IllegalArgumentException("operationId must be non-empty and at most 64 characters.");
        }
        return checked;
    }
}
