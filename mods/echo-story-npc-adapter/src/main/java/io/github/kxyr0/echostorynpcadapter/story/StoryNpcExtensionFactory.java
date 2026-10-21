package io.github.kxyr0.echostorynpcadapter.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import io.github.kxyr0.echostorynpcadapter.action.NpcActionTypes;
import java.util.List;
import java.util.Objects;

public final class StoryNpcExtensionFactory implements ForgeStoryExtensionFactory {
    public static final NamespacedId EXTENSION_ID = NamespacedId.of("echo_story_npc_adapter:bridge");
    private static final ActionHandlerCapabilities CAPABILITIES =
            new ActionHandlerCapabilities(true, false, true, true);
    private static final String VERSION = "0.1.0-SNAPSHOT";

    private final AdapterSessions sessions;
    private final StoryExtensionDescriptor descriptor = new StoryExtensionDescriptor(
            EXTENSION_ID,
            "Echo Story NPC Adapter",
            VERSION,
            1,
            0,
            NpcActionTypes.ALL.stream()
                    .map(action -> new ActionHandlerDescriptor(action, CAPABILITIES))
                    .toList());

    public StoryNpcExtensionFactory(AdapterSessions sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public StoryExtensionSession create(ForgeStoryExtensionContext context) {
        return new StoryNpcExtensionSession(context.server(), descriptor, sessions);
    }
}
