package io.github.kxyr0.echostoryengine.testextension;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;

final class TestExtensionFactory implements ForgeStoryExtensionFactory {
    static final NamespacedId EXTENSION_ID = NamespacedId.of("echo_story_test:extension");
    static final NamespacedId SUCCESS_ACTION = NamespacedId.of("echo_story_test:success");
    static final NamespacedId FAILURE_ACTION = NamespacedId.of("echo_story_test:failure");

    private final StoryExtensionDescriptor descriptor = new StoryExtensionDescriptor(
            EXTENSION_ID,
            "Echo Story Extension Test",
            "1.0.0",
            1,
            0,
            List.of(
                    new ActionHandlerDescriptor(SUCCESS_ACTION, ActionHandlerCapabilities.recoverableAsync()),
                    new ActionHandlerDescriptor(FAILURE_ACTION, ActionHandlerCapabilities.recoverableAsync())));

    @Override
    public StoryExtensionDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public StoryExtensionSession create(ForgeStoryExtensionContext context) {
        return new TestExtensionSession(descriptor);
    }
}
