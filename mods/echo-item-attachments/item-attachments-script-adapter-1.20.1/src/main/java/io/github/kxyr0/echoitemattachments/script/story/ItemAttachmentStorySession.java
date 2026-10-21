package io.github.kxyr0.echoitemattachments.script.story;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import net.minecraft.server.MinecraftServer;

final class ItemAttachmentStorySession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final List<ItemAttachmentStoryAction> actions;

    ItemAttachmentStorySession(MinecraftServer server, StoryExtensionDescriptor descriptor) {
        this.descriptor = descriptor;
        actions = List.of(
                new ItemAttachmentStoryAction(server, ItemAttachmentStoryExtensionFactory.TAKE, "take"),
                new ItemAttachmentStoryAction(server, ItemAttachmentStoryExtensionFactory.MOVE, "move"),
                new ItemAttachmentStoryAction(server, ItemAttachmentStoryExtensionFactory.DROP, "drop"),
                new ItemAttachmentStoryAction(server, ItemAttachmentStoryExtensionFactory.THROW, "throw"));
    }

    @Override public StoryExtensionDescriptor descriptor() { return descriptor; }
    @Override public List<StoryActionHandler> handlers() { return List.copyOf(actions); }
    void tick() { actions.forEach(ItemAttachmentStoryAction::tick); }
    @Override public void close() { actions.forEach(ItemAttachmentStoryAction::close); }
}
