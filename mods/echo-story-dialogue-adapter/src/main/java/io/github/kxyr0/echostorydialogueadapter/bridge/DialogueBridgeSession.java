package io.github.kxyr0.echostorydialogueadapter.bridge;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;

/**
 * One active server-scoped Story extension session.
 */
public final class DialogueBridgeSession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final DialoguePlayActionHandler handler;

    DialogueBridgeSession(MinecraftServer server, StoryExtensionDescriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.handler = new DialoguePlayActionHandler(server);
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return List.of(handler);
    }

    @Override
    public void close() {
        handler.close();
    }
}
