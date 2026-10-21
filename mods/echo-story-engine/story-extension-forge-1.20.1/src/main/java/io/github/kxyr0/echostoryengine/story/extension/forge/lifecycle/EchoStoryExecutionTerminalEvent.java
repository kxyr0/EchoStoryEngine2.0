package io.github.kxyr0.echostoryengine.story.extension.forge.lifecycle;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoStoryExecutionTerminalEvent extends Event {
    private final MinecraftServer server;
    private final ExecutionId executionId;
    private final StoryExecutionPhase phase;

    public EchoStoryExecutionTerminalEvent(MinecraftServer server, ExecutionId executionId,
                                           StoryExecutionPhase phase) {
        this.server = Objects.requireNonNull(server, "server");
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.phase = Objects.requireNonNull(phase, "phase");
        if (phase != StoryExecutionPhase.FINISHED && phase != StoryExecutionPhase.FAILED) {
            throw new IllegalArgumentException("Story terminal event requires FINISHED or FAILED phase.");
        }
    }

    public MinecraftServer server() { return server; }
    public ExecutionId executionId() { return executionId; }
    public StoryExecutionPhase phase() { return phase; }
}

