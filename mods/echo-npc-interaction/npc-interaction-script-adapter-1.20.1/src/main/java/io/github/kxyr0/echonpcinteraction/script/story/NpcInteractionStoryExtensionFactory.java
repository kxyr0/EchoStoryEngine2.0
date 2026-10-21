package io.github.kxyr0.echonpcinteraction.script.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

public final class NpcInteractionStoryExtensionFactory implements ForgeStoryExtensionFactory {
    public static final NamespacedId INTERACT = NamespacedId.of("echo_npc_interaction:interact");
    public static final NamespacedId STOP = NamespacedId.of("echo_npc_interaction:stop");
    public static final NamespacedId STAND_UP = NamespacedId.of("echo_npc_interaction:stand_up");
    private final Map<MinecraftServer, NpcInteractionStorySession> sessions = new LinkedHashMap<>();

    @Override
    public StoryExtensionDescriptor descriptor() {
        return new StoryExtensionDescriptor(NamespacedId.of("echo_npc_interaction:story_bridge"),
                "Echo NPC Interaction Script Adapter", "0.2.0", 1, 0, List.of(
                new ActionHandlerDescriptor(INTERACT, ActionHandlerCapabilities.recoverableAsync()),
                new ActionHandlerDescriptor(STOP, ActionHandlerCapabilities.basic()),
                new ActionHandlerDescriptor(STAND_UP, ActionHandlerCapabilities.basic())));
    }

    @Override
    public NpcInteractionStorySession create(ForgeStoryExtensionContext context) {
        return sessions.computeIfAbsent(context.server(),
                server -> new NpcInteractionStorySession(server, descriptor()));
    }

    public void tick(MinecraftServer server) {
        NpcInteractionStorySession session = sessions.get(server);
        if (session != null) {
            session.tick();
        }
    }

    public void close(MinecraftServer server) {
        NpcInteractionStorySession session = sessions.remove(server);
        if (session != null) {
            session.close();
        }
    }
}
