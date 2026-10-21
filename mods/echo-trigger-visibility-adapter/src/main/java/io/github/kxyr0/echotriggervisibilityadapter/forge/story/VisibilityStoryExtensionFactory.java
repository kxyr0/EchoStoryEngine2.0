package io.github.kxyr0.echotriggervisibilityadapter.forge.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.Arrays;
import net.minecraft.server.MinecraftServer;

public final class VisibilityStoryExtensionFactory implements ForgeStoryExtensionFactory {
    private VisibilityStorySession session;

    @Override
    public StoryExtensionDescriptor descriptor() {
        return new StoryExtensionDescriptor(
                NamespacedId.of("echo_visibility:story_actions"),
                "Echo Visibility Story Actions",
                "0.2.0",
                1,
                0,
                Arrays.stream(VisibilityStoryActionKind.values())
                        .map(kind -> new ActionHandlerDescriptor(
                                kind.actionType(), ActionHandlerCapabilities.recoverableAsync()))
                        .toList());
    }

    @Override
    public VisibilityStorySession create(ForgeStoryExtensionContext context) {
        VisibilityStorySession created = new VisibilityStorySession(
                context.server(), descriptor(), () -> detach(context.server()));
        session = created;
        return created;
    }

    public void tick(MinecraftServer server) {
        if (session != null && session.server() == server) {
            session.tick();
        }
    }

    public void close(MinecraftServer server) {
        if (session != null && session.server() == server) {
            VisibilityStorySession closing = session;
            session = null;
            closing.close();
        }
    }

    private void detach(MinecraftServer server) {
        if (session != null && session.server() == server) {
            session = null;
        }
    }
}
