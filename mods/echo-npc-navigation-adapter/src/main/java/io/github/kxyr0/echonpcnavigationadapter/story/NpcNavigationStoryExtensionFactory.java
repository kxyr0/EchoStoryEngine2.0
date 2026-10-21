package io.github.kxyr0.echonpcnavigationadapter.story;

import io.github.kxyr0.echonpcnavigationadapter.runtime.AdapterSessions;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;
import java.util.Objects;

public final class NpcNavigationStoryExtensionFactory implements ForgeStoryExtensionFactory {
    static final NamespacedId MOVE = NamespacedId.of("echo_navigation:npc_move");
    static final NamespacedId STOP = NamespacedId.of("echo_navigation:npc_stop");
    private final AdapterSessions sessions;

    public NpcNavigationStoryExtensionFactory(AdapterSessions sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return new StoryExtensionDescriptor(NamespacedId.of("echo_navigation:npc_story_bridge"),
                "Echo NPC Navigation Adapter", "0.1.0", 1, 0, List.of(
                new ActionHandlerDescriptor(MOVE, ActionHandlerCapabilities.recoverableAsync()),
                new ActionHandlerDescriptor(STOP, ActionHandlerCapabilities.basic())));
    }

    @Override
    public NpcNavigationStorySession create(ForgeStoryExtensionContext context) {
        return new NpcNavigationStorySession(context.server(), descriptor(), sessions);
    }
}
