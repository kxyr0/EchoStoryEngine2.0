package io.github.kxyr0.echoobservation.forge.runtime;

import io.github.kxyr0.echoobservation.api.action.ObservationActionContext;
import io.github.kxyr0.echoobservation.api.action.ObservationActionExecutor;
import io.github.kxyr0.echoobservation.api.action.ObservationScriptAction;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import net.minecraft.server.MinecraftServer;

final class ObservationStoryStartExecutor implements ObservationActionExecutor {
    private final MinecraftServer server;

    ObservationStoryStartExecutor(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String target() {
        return "Story.Start";
    }

    @Override
    public boolean execute(ObservationActionContext context, ObservationScriptAction action) {
        if (action.arguments().isEmpty()) return false;
        var lookup = EchoStoryExecutionBridge.lookup(server);
        if (lookup.starter().isEmpty()) return false;
        try {
            StoryId storyId = StoryId.of(action.arguments().get(0).value());
            ExecutionId executionId = ExecutionId.of("echo_observation:" + context.executionId().value()
                    + "/player/" + context.playerId());
            lookup.starter().orElseThrow().start(storyId, executionId);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
