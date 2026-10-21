package io.github.kxyr0.echostoryengine.testextension;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import java.util.List;
import java.util.Objects;

final class TestExtensionSession implements StoryExtensionSession {
    private final StoryExtensionDescriptor descriptor;
    private final List<StoryActionHandler> handlers;
    private boolean closed;

    TestExtensionSession(StoryExtensionDescriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.handlers = List.of(
                new TestStoryActionHandler(TestExtensionFactory.SUCCESS_ACTION, true),
                new TestStoryActionHandler(TestExtensionFactory.FAILURE_ACTION, false));
    }

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<StoryActionHandler> handlers() {
        return handlers;
    }

    @Override
    public void close() {
        closed = true;
    }

    boolean closed() {
        return closed;
    }
}
