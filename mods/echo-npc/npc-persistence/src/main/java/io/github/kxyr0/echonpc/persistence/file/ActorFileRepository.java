package io.github.kxyr0.echonpc.persistence.file;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.persistence.ActorRepositoryResult;
import io.github.kxyr0.echonpc.core.persistence.ActorStateRepository;
import io.github.kxyr0.echonpc.persistence.schema.ActorSaveCodec;
import io.github.kxyr0.echonpc.persistence.schema.ActorSaveMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Portable synchronous actor file repository.
 */
public final class ActorFileRepository implements ActorStateRepository {
    private final ActorFileLayout layout;
    private final ActorSaveMapper mapper = new ActorSaveMapper();
    private final ActorSaveCodec codec = new ActorSaveCodec();
    private final ActorAtomicFiles atomic;
    private final ActorFileReader reader;
    private final ActorRepositoryLimits limits;

    public ActorFileRepository(Path root) {
        this(root, ActorRepositoryLimits.standard());
    }

    public ActorFileRepository(Path root, ActorRepositoryLimits limits) {
        this.layout = new ActorFileLayout(root);
        this.atomic = new ActorAtomicFiles(layout, codec);
        this.reader = new ActorFileReader(layout, mapper, codec);
        this.limits = java.util.Objects.requireNonNull(limits, "limits");
    }

    @Override
    public ActorRepositoryResult.Load load(ActorId actorId) {
        return reader.load(actorId);
    }

    @Override
    public ActorRepositoryResult.LoadAll loadAll(int limit) {
        int bounded = Math.max(0, Math.min(Math.min(limit, limits.maxActorRecords()), limits.maxActorRecords()));
        return reader.loadAll(bounded);
    }

    @Override
    public ActorRepositoryResult.Save create(ActorDesiredState desired) {
        ActorRepositoryResult.Load current = load(desired.actorId());
        if (loadFailed(current)) {
            return failedSave(current);
        }
        if (current.status() == ActorRepositoryResult.LoadStatus.FOUND
                && current.actor().orElseThrow().presence() == ActorPresence.ACTIVE) {
            return new ActorRepositoryResult.Save(ActorRepositoryResult.SaveStatus.REVISION_CONFLICT,
                    current.actor(), "Актёр уже существует.");
        }
        long revision = current.actor().map(ActorDesiredState::repositoryRevision).orElse(0L) + 1L;
        return save(desired.withRepositoryRevision(revision), ActorRepositoryResult.SaveStatus.CREATED);
    }

    @Override
    public ActorRepositoryResult.Save update(ActorDesiredState desired, long expectedRepositoryRevision) {
        ActorRepositoryResult.Load current = load(desired.actorId());
        if (loadFailed(current)) {
            return failedSave(current);
        }
        if (current.status() != ActorRepositoryResult.LoadStatus.FOUND
                || current.actor().orElseThrow().repositoryRevision() != expectedRepositoryRevision) {
            return new ActorRepositoryResult.Save(ActorRepositoryResult.SaveStatus.REVISION_CONFLICT,
                    current.actor(), "Repository revision conflict.");
        }
        return save(desired.withRepositoryRevision(expectedRepositoryRevision + 1L),
                ActorRepositoryResult.SaveStatus.UPDATED);
    }

    @Override
    public ActorRepositoryResult.Removal tombstone(ActorDesiredState absent, long expectedRepositoryRevision) {
        ActorRepositoryResult.Load current = load(absent.actorId());
        if (loadFailed(current)) {
            return failedRemoval(current);
        }
        if (current.status() == ActorRepositoryResult.LoadStatus.FOUND
                && current.actor().orElseThrow().repositoryRevision() != expectedRepositoryRevision) {
            return new ActorRepositoryResult.Removal(ActorRepositoryResult.RemovalStatus.REVISION_CONFLICT,
                    current.actor(), "Repository revision conflict.");
        }
        long revision = current.actor().map(ActorDesiredState::repositoryRevision).orElse(0L) + 1L;
        ActorRepositoryResult.Save saved = save(absent.withRepositoryRevision(revision),
                ActorRepositoryResult.SaveStatus.UPDATED);
        return new ActorRepositoryResult.Removal(saved.status() == ActorRepositoryResult.SaveStatus.UPDATED
                ? ActorRepositoryResult.RemovalStatus.TOMBSTONED : ActorRepositoryResult.RemovalStatus.FAILED,
                saved.actor(), saved.message());
    }

    @Override
    public ActorRepositoryResult.Removal remove(ActorId actorId, long expectedRepositoryRevision) {
        ActorRepositoryResult.Load current = load(actorId);
        if (loadFailed(current)) {
            return failedRemoval(current);
        }
        if (current.status() == ActorRepositoryResult.LoadStatus.NOT_FOUND) {
            return new ActorRepositoryResult.Removal(ActorRepositoryResult.RemovalStatus.NOT_FOUND,
                    Optional.empty(), "Actor record отсутствует.");
        }
        if (current.status() != ActorRepositoryResult.LoadStatus.FOUND
                || current.actor().orElseThrow().repositoryRevision() != expectedRepositoryRevision
                || current.actor().orElseThrow().presence() != ActorPresence.ABSENT) {
            return new ActorRepositoryResult.Removal(ActorRepositoryResult.RemovalStatus.REVISION_CONFLICT,
                    current.actor(), "Удалять можно только ожидаемый ABSENT record.");
        }
        return delete(actorId);
    }

    @Override
    public ActorRepositoryResult.Health health() {
        if (!Files.isDirectory(layout.root())) {
            return new ActorRepositoryResult.Health(true, 0, 0, "Repository root еще не создан.");
        }
        int primaries = reader.count("*.json");
        int backups = reader.count("*.json.bak");
        return new ActorRepositoryResult.Health(true, primaries, backups, "Repository доступен.");
    }

    private ActorRepositoryResult.Save save(ActorDesiredState desired, ActorRepositoryResult.SaveStatus status) {
        try {
            atomic.write(desired.actorId(), codec.encode(mapper.toRecord(desired)));
            return new ActorRepositoryResult.Save(status, Optional.of(desired), "Actor record saved.");
        } catch (IOException | RuntimeException exception) {
            return new ActorRepositoryResult.Save(ActorRepositoryResult.SaveStatus.FAILED,
                    Optional.empty(), exception.getClass().getSimpleName());
        }
    }

    private ActorRepositoryResult.Removal delete(ActorId actorId) {
        try {
            atomic.remove(actorId);
            return new ActorRepositoryResult.Removal(ActorRepositoryResult.RemovalStatus.REMOVED,
                    Optional.empty(), "Actor record удалён.");
        } catch (IOException exception) {
            return new ActorRepositoryResult.Removal(ActorRepositoryResult.RemovalStatus.FAILED,
                    Optional.empty(), exception.getClass().getSimpleName());
        }
    }

    private boolean loadFailed(ActorRepositoryResult.Load current) {
        return current.status() == ActorRepositoryResult.LoadStatus.CORRUPT
                || current.status() == ActorRepositoryResult.LoadStatus.UNSUPPORTED_SCHEMA
                || current.status() == ActorRepositoryResult.LoadStatus.FAILED;
    }

    private ActorRepositoryResult.Save failedSave(ActorRepositoryResult.Load current) {
        return new ActorRepositoryResult.Save(ActorRepositoryResult.SaveStatus.FAILED,
                current.actor(), current.message());
    }

    private ActorRepositoryResult.Removal failedRemoval(ActorRepositoryResult.Load current) {
        return new ActorRepositoryResult.Removal(ActorRepositoryResult.RemovalStatus.FAILED,
                current.actor(), current.message());
    }

}
