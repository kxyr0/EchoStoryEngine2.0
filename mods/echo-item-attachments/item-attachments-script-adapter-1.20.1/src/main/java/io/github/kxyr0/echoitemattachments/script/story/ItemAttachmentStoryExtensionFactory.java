package io.github.kxyr0.echoitemattachments.script.story;

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

public final class ItemAttachmentStoryExtensionFactory implements ForgeStoryExtensionFactory {
    public static final NamespacedId TAKE = NamespacedId.of("echo_item_attachments:take");
    public static final NamespacedId MOVE = NamespacedId.of("echo_item_attachments:move");
    public static final NamespacedId DROP = NamespacedId.of("echo_item_attachments:drop");
    public static final NamespacedId THROW = NamespacedId.of("echo_item_attachments:throw");
    private final Map<MinecraftServer, ItemAttachmentStorySession> sessions = new LinkedHashMap<>();

    @Override
    public StoryExtensionDescriptor descriptor() {
        var capability = ActionHandlerCapabilities.recoverableAsync();
        return new StoryExtensionDescriptor(NamespacedId.of("echo_item_attachments:story_bridge"),
                "Echo Item Attachments Script Adapter", "0.1.0", 1, 0, List.of(
                new ActionHandlerDescriptor(TAKE, capability),
                new ActionHandlerDescriptor(MOVE, capability),
                new ActionHandlerDescriptor(DROP, capability),
                new ActionHandlerDescriptor(THROW, capability)));
    }

    @Override
    public ItemAttachmentStorySession create(ForgeStoryExtensionContext context) {
        return sessions.computeIfAbsent(context.server(),
                server -> new ItemAttachmentStorySession(server, descriptor()));
    }

    public void tick(MinecraftServer server) {
        ItemAttachmentStorySession session = sessions.get(server);
        if (session != null) session.tick();
    }

    public void close(MinecraftServer server) {
        ItemAttachmentStorySession session = sessions.remove(server);
        if (session != null) session.close();
    }
}
