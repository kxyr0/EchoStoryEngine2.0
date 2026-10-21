package io.github.kxyr0.echoaudio.forge.story;

import io.github.kxyr0.echostoryengine.story.api.identity.*;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.*;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.*;
import io.github.kxyr0.echostoryengine.story.extension.forge.*;
import java.util.List;

public final class AudioStoryFactory implements ForgeStoryExtensionFactory {
    public static final NamespacedId ACTION = NamespacedId.of("echo_audio:cue");
    private static final StoryExtensionApiVersion API = StoryExtensionApiVersion.current();
    private static final StoryExtensionDescriptor DESCRIPTOR = new StoryExtensionDescriptor(
            NamespacedId.of("echo_audio:story"), "Echo Audio", "0.1.0", API.major(), API.minor(),
            List.of(new ActionHandlerDescriptor(ACTION, ActionHandlerCapabilities.recoverableAsync())));
    @Override public StoryExtensionDescriptor descriptor() { return DESCRIPTOR; }
    @Override public StoryExtensionSession create(ForgeStoryExtensionContext context) {
        AudioStoryHandler handler = new AudioStoryHandler(context.server());
        return new StoryExtensionSession() {
            @Override public StoryExtensionDescriptor descriptor() { return DESCRIPTOR; }
            @Override public List<StoryActionHandler> handlers() { return List.of(handler); }
            @Override public void close() { handler.close(); }
        };
    }
}
