package io.github.kxyr0.echotime.script.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;

public final class TimeStoryExtensionFactory implements ForgeStoryExtensionFactory {
    static final List<NamespacedId> ACTIONS = List.of(
            "set", "add", "pause", "resume", "wait_until", "is", "is_before", "is_after", "is_between")
            .stream().map(value -> NamespacedId.of("echo_time:" + value)).toList();
    private final StoryExtensionDescriptor descriptor = new StoryExtensionDescriptor(
            NamespacedId.of("echo_time:story_script"), "Echo Time Story Script", "0.1.0", 1, 0,
            ACTIONS.stream().map(action -> new ActionHandlerDescriptor(
                    action, ActionHandlerCapabilities.recoverableAsync())).toList());

    @Override public StoryExtensionDescriptor descriptor() { return descriptor; }

    @Override
    public StoryExtensionSession create(ForgeStoryExtensionContext context) {
        return new TimeStorySession(descriptor, context.server());
    }
}
