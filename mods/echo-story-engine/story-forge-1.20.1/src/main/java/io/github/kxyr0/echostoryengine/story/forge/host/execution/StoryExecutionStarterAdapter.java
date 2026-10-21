package io.github.kxyr0.echostoryengine.story.forge.host.execution;

import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupResult;
import io.github.kxyr0.echostoryengine.story.forge.host.ForgeServerHost;
import java.util.function.Consumer;

public record StoryExecutionStarterAdapter(ForgeServerHost host)
        implements EchoStoryExecutionLookupResult.StoryExecutionStarter {
    @Override
    public void start(StoryId storyId, ExecutionId executionId) {
        host.startExecution(storyId, executionId);
    }
    @Override
    public void startDurable(StoryId storyId, ExecutionId executionId, Consumer<Boolean> committed) {
        host.startDurable(storyId, executionId, committed);
    }
}
