package io.github.kxyr0.echostoryengine.story.core.port;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;

/**
 * Persistence boundary for story execution checkpoints.
 *
 * <p>The port deliberately says nothing about NBT, SavedData, JSON, files, databases, or atomic write strategy.
 * Platform adapters own those decisions behind this boundary.
 */
public interface ExecutionStateRepository {
    ExecutionLoadResult load(ExecutionId executionId);

    ExecutionSaveResult save(ExecutionCheckpoint checkpoint, int expectedRevision);
}
