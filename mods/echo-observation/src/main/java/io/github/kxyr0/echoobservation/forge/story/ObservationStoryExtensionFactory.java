package io.github.kxyr0.echoobservation.forge.story;

import io.github.kxyr0.echoobservation.api.runtime.ObservationService;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;

final class ObservationStoryExtensionFactory implements ForgeStoryExtensionFactory {
    private final ObservationService service;
    private final StoryExtensionDescriptor descriptor;

    ObservationStoryExtensionFactory(ObservationService service) {
        this.service = service;
        StoryExtensionApiVersion version = StoryExtensionApiVersion.current();
        List<ActionHandlerDescriptor> actions = ObservationStoryOperation.all().stream()
                .map(value -> new ActionHandlerDescriptor(value.actionType(),
                        ActionHandlerCapabilities.recoverableAsync())).toList();
        descriptor = new StoryExtensionDescriptor(NamespacedId.of("echo_observation:story_extension"),
                "Echo Observation Story Extension", "1.0.0", version.major(), version.minor(), actions);
    }

    @Override public StoryExtensionDescriptor descriptor() { return descriptor; }

    @Override
    public ObservationStorySession create(ForgeStoryExtensionContext context) {
        return new ObservationStorySession(descriptor, service);
    }
}
