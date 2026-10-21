package io.github.kxyr0.echonpc.core.persistence;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;

/**
 * Core persistence port. Implementations own storage details outside npc-core.
 */
public interface ActorStateRepository {
    ActorRepositoryResult.Load load(ActorId actorId);

    ActorRepositoryResult.LoadAll loadAll(int limit);

    ActorRepositoryResult.Save create(ActorDesiredState desired);

    ActorRepositoryResult.Save update(ActorDesiredState desired, long expectedRepositoryRevision);

    ActorRepositoryResult.Removal tombstone(ActorDesiredState absent, long expectedRepositoryRevision);

    ActorRepositoryResult.Removal remove(ActorId actorId, long expectedRepositoryRevision);

    default ActorRepositoryResult.Health health() {
        return new ActorRepositoryResult.Health(true, 0, 0, "health не предоставлен.");
    }
}
