package io.github.kxyr0.echostoryengine.story.extension.forge;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result for public Forge-side Story execution starter lookup.
 */
public record EchoStoryExecutionLookupResult(
        Status status,
        Optional<StoryExecutionStarter> starter,
        String message) {
    public EchoStoryExecutionLookupResult {
        status = Objects.requireNonNull(status, "status");
        starter = Objects.requireNonNull(starter, "starter");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        RESOLVED,
        UNAVAILABLE,
        DUPLICATE_PROVIDER
    }

    @FunctionalInterface
    public interface StoryExecutionStarter {
        void start(StoryId storyId, ExecutionId executionId);

        /** Called on the server thread only after the execution has a committed start checkpoint. */
        default void startDurable(StoryId storyId, ExecutionId executionId,
                java.util.function.Consumer<Boolean> committed) {
            committed.accept(false);
        }
    }
}
