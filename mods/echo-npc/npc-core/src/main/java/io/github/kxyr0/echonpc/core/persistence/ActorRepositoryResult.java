package io.github.kxyr0.echonpc.core.persistence;

import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit repository outcomes; expected conditions are statuses, not unchecked exceptions.
 */
public final class ActorRepositoryResult {
    private ActorRepositoryResult() {
    }

    public enum LoadStatus {
        FOUND,
        NOT_FOUND,
        CORRUPT,
        UNSUPPORTED_SCHEMA,
        FAILED
    }

    public enum SaveStatus {
        CREATED,
        UPDATED,
        REVISION_CONFLICT,
        FAILED
    }

    public enum RemovalStatus {
        TOMBSTONED,
        REMOVED,
        NOT_FOUND,
        REVISION_CONFLICT,
        FAILED
    }

    public record Load(LoadStatus status, Optional<ActorDesiredState> actor, String message) {
        public Load {
            status = Objects.requireNonNull(status, "status");
            actor = Objects.requireNonNull(actor, "actor");
            message = Objects.requireNonNull(message, "message");
        }
    }

    public record LoadAll(List<Load> records, boolean truncated, List<String> warnings) {
        public LoadAll {
            records = List.copyOf(Objects.requireNonNull(records, "records"));
            warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
        }
    }

    public record Save(SaveStatus status, Optional<ActorDesiredState> actor, String message) {
        public Save {
            status = Objects.requireNonNull(status, "status");
            actor = Objects.requireNonNull(actor, "actor");
            message = Objects.requireNonNull(message, "message");
        }
    }

    public record Removal(RemovalStatus status, Optional<ActorDesiredState> actor, String message) {
        public Removal {
            status = Objects.requireNonNull(status, "status");
            actor = Objects.requireNonNull(actor, "actor");
            message = Objects.requireNonNull(message, "message");
        }
    }

    public record Health(boolean rootAvailable, int primaryRecords, int backupRecords, String message) {
        public Health {
            if (primaryRecords < 0 || backupRecords < 0) {
                throw new IllegalArgumentException("Repository health counters must not be negative.");
            }
            message = Objects.requireNonNull(message, "message");
        }
    }
}
