package io.github.kxyr0.echostoryanimationadapter.bridge;

import io.github.kxyr0.echostoryanimationadapter.runtime.AdapterSessions;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;
import java.util.Objects;

/**
 * Server-scoped Story extension factory registered through the public Forge bridge.
 */
public final class AnimationBridgeExtensionFactory implements ForgeStoryExtensionFactory {
    private static final StoryExtensionApiVersion API_VERSION = StoryExtensionApiVersion.current();
    private static final ActionHandlerCapabilities CAPABILITIES = ActionHandlerCapabilities.recoverableAsync();
    private static final StoryExtensionDescriptor DESCRIPTOR = new StoryExtensionDescriptor(
            AnimationBridgeIds.EXTENSION_ID,
            "Echo Story Animation Adapter",
            AnimationBridgeIds.IMPLEMENTATION_VERSION,
            API_VERSION.major(),
            API_VERSION.minor(),
            AnimationBridgeIds.ACTION_TYPES.stream()
                    .map(action -> new ActionHandlerDescriptor(action, CAPABILITIES))
                    .toList());
    private final AdapterSessions sessions;

    public AnimationBridgeExtensionFactory(AdapterSessions sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public AnimationBridgeSession create(ForgeStoryExtensionContext context) {
        return new AnimationBridgeSession(context.server(), DESCRIPTOR, sessions);
    }
}
