package io.github.kxyr0.echostoryengine.story.core.port;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.util.Optional;

/**
 * Persistence boundary for story execution state.
 *
 * <p>The port deliberately says nothing about NBT, SavedData, JSON, files, databases, or atomic write strategy.
 * Platform adapters own those decisions behind this boundary.
 */
public interface ExecutionStateRepository {
    Optional<StoryExecutionState> load(ExecutionId executionId);

    void save(StoryExecutionState state);
}
