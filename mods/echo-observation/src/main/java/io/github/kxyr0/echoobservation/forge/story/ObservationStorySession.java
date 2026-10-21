package io.github.kxyr0.echoobservation.forge.story;

import io.github.kxyr0.echoobservation.api.runtime.ObservationService;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;

final class ObservationStorySession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final List<StoryActionHandler> handlers;

    ObservationStorySession(StoryExtensionDescriptor descriptor, ObservationService service) {
        this.descriptor = descriptor;
        handlers = ObservationStoryOperation.all().stream()
                .map(operation -> new ObservationStoryActionHandler(service, operation))
                .map(StoryActionHandler.class::cast).toList();
    }

    @Override public StoryExtensionDescriptor descriptor() { return descriptor; }
    @Override public List<StoryActionHandler> handlers() { return handlers; }
    @Override public void close() { }
}
