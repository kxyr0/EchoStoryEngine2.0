package io.github.kxyr0.echostoryengine.story.forge.execution;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;

/**
 * Boundary for abstract external commands after durable save.
 */
@FunctionalInterface
public interface ExternalCommandSink {
    void dispatch(StoryExecutionState state, ExternalCommand command);

    default void recover(StoryExecutionState state, ExternalCommand command) {
        dispatch(state, command);
    }
}
