package io.github.kxyr0.echonpcbackpack.script.story;

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

public final class NpcBackpackStoryExtensionFactory implements ForgeStoryExtensionFactory {
    public static final NamespacedId EQUIP = NamespacedId.of("echo_npc_sophisticated_backpacks:equip");
    public static final NamespacedId UNEQUIP = NamespacedId.of("echo_npc_sophisticated_backpacks:unequip");
    private final Map<MinecraftServer, NpcBackpackStorySession> sessions = new LinkedHashMap<>();

    @Override
    public StoryExtensionDescriptor descriptor() {
        return new StoryExtensionDescriptor(NamespacedId.of("echo_npc_sophisticated_backpacks:story_bridge"),
                "Echo NPC Sophisticated Backpacks Script Adapter", "0.2.0", 1, 0, List.of(
                new ActionHandlerDescriptor(EQUIP, ActionHandlerCapabilities.recoverableAsync()),
                new ActionHandlerDescriptor(UNEQUIP, ActionHandlerCapabilities.recoverableAsync())));
    }

    @Override
    public NpcBackpackStorySession create(ForgeStoryExtensionContext context) {
        return sessions.computeIfAbsent(context.server(),
                server -> new NpcBackpackStorySession(server, descriptor()));
    }

    public void tick(MinecraftServer server) {
        NpcBackpackStorySession session = sessions.get(server);
        if (session != null) {
            session.tick();
        }
    }

    public void close(MinecraftServer server) {
        NpcBackpackStorySession session = sessions.remove(server);
        if (session != null) {
            session.close();
        }
    }
}
