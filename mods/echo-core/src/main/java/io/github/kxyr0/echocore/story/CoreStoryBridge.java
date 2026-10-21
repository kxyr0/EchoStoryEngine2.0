package io.github.kxyr0.echocore.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory;
import java.util.List;
import net.minecraftforge.common.MinecraftForge;

public final class CoreStoryBridge implements ForgeStoryExtensionFactory {
    public static final NamespacedId ACTION = NamespacedId.of("echo_core:set_flag");
    private static final StoryExtensionApiVersion API = StoryExtensionApiVersion.current();
    private static final StoryExtensionDescriptor DESCRIPTOR = new StoryExtensionDescriptor(
            NamespacedId.of("echo_core:features"), "Echo Core feature flags", "0.2.0", API.major(), API.minor(),
            List.of(new ActionHandlerDescriptor(ACTION, new ActionHandlerCapabilities(true, false, false, true))));
    public static void register() { MinecraftForge.EVENT_BUS.addListener(CoreStoryBridge::registration); }
    private static void registration(EchoStoryExtensionRegistrationEvent event) { event.registrar().register(new CoreStoryBridge()); }
    @Override public StoryExtensionDescriptor descriptor() { return DESCRIPTOR; }
    @Override public StoryExtensionSession create(ForgeStoryExtensionContext context) {
        return new StoryExtensionSession() {
            private final StoryActionHandler handler = new FeatureStoryAction(context.server());
            @Override public StoryExtensionDescriptor descriptor() { return DESCRIPTOR; }
            @Override public List<StoryActionHandler> handlers() { return List.of(handler); }
            @Override public void close() { }
        };
    }
}
