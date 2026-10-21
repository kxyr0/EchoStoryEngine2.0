package io.github.kxyr0.echoplayersleep.script;

import io.github.kxyr0.echoplayersleep.forge.api.PlayerSleepServiceBridge;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import java.util.Objects;

public final class SleepStoryExtensionSession implements StoryExtensionSession {
    private final PlayerSleepServiceBridge services;
    private final StoryExtensionDescriptor descriptor;

    public SleepStoryExtensionSession(PlayerSleepServiceBridge services, String implementationVersion) {
        this.services = Objects.requireNonNull(services, "services");
        descriptor = new StoryExtensionDescriptor(NamespacedId.of("echo_player_sleep:sleep"),
                "Echo Player Sleep", implementationVersion, 1, 0, SleepActionCatalog.descriptors());
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return SleepActionCatalog.all().stream()
                .map(action -> new SleepActionHandler(action, services))
                .map(StoryActionHandler.class::cast)
                .toList();
    }

    @Override
    public void close() {
    }
}
