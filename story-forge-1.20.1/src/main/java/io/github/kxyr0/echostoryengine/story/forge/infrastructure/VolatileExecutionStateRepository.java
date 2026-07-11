package io.github.kxyr0.echostoryengine.story.forge.infrastructure;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-lifetime state repository used until a real persistence adapter is designed.
 *
 * <p>This adapter proves the port boundary without claiming durable save/load behavior.
 */
public final class VolatileExecutionStateRepository implements ExecutionStateRepository {
    private final Map<ExecutionId, StoryExecutionState> states = new HashMap<>();

    @Override
    public synchronized Optional<StoryExecutionState> load(ExecutionId executionId) {
        return Optional.ofNullable(states.get(Objects.requireNonNull(executionId, "executionId")));
    }

    @Override
    public synchronized void save(StoryExecutionState state) {
        StoryExecutionState checked = Objects.requireNonNull(state, "state");
        states.put(checked.executionId(), checked);
    }
}
