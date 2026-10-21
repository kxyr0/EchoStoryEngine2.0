package io.github.kxyr0.echostoryengine.story.forge.host;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

final class StoryStartCommandSupport {
    static final NamespacedId ACTION_TYPE = NamespacedId.of("echo_story:start");

    boolean handles(ExternalCommand command) {
        return ACTION_TYPE.equals(command.actionType());
    }

    ExternalActionResultCategory apply(StoryStartCommandContext context) {
        try {
            StoryId storyId = StoryId.of(context.command().payload().getOrDefault("storyId", ""));
            if (!context.storyExists().test(storyId)) {
                return ExternalActionResultCategory.FAILURE;
            }
            context.storyStarter().accept(storyId, executionId(context.command()));
            return ExternalActionResultCategory.SUCCESS;
        } catch (RuntimeException exception) {
            return ExternalActionResultCategory.FAILURE;
        }
    }

    private ExecutionId executionId(ExternalCommand command) {
        String explicit = command.payload().getOrDefault("executionId", "").trim();
        if (!explicit.isEmpty()) {
            return ExecutionId.of(explicit);
        }
        return ExecutionId.of(command.actionExecutionId().asString() + "/story");
    }

    record StoryStartCommandContext(
            StoryExecutionState state,
            ExternalCommand command,
            Predicate<StoryId> storyExists,
            BiConsumer<StoryId, ExecutionId> storyStarter) {
        StoryStartCommandContext {
            state = Objects.requireNonNull(state, "state");
            command = Objects.requireNonNull(command, "command");
            storyExists = Objects.requireNonNull(storyExists, "storyExists");
            storyStarter = Objects.requireNonNull(storyStarter, "storyStarter");
        }
    }
}
