package io.github.kxyr0.echodialoguecutscene.forge.screen;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import net.minecraft.server.MinecraftServer;

final class ScreenStoryActionHandler implements StoryActionHandler {
    private final MinecraftServer server;
    private final ScreenCommandScheduler scheduler;

    ScreenStoryActionHandler(MinecraftServer server, ScreenCommandScheduler scheduler) {
        this.server = server;
        this.scheduler = scheduler;
    }

    @Override
    public NamespacedId actionType() {
        return ScreenStoryIds.SET_BLACK;
    }

    @Override
    public ActionInvocationResult start(ActionStartRequest request, ActionCompletionSink sink) {
        return scheduler.startStory(server, ScreenCommandScheduler.StoryContext.from(request), request.payload(), sink);
    }

    @Override
    public ActionInvocationResult recover(ActionRecoveryRequest request, ActionCompletionSink sink) {
        return scheduler.startStory(server, ScreenCommandScheduler.StoryContext.from(request), request.payload(), sink);
    }

    @Override
    public ActionInvocationResult cancel(ActionCancelRequest request) {
        return scheduler.cancelStory(request);
    }
}
