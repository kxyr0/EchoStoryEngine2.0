package io.github.kxyr0.echostoryengine.story.extension.forge;

import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupResult.StoryExecutionStarter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Server-scoped lookup event for public Story execution start access.
 */
public final class EchoStoryExecutionLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<StoryExecutionStarter> providers = new ArrayList<>();

    public EchoStoryExecutionLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void register(StoryExecutionStarter starter) {
        providers.add(Objects.requireNonNull(starter, "starter"));
    }

    public List<StoryExecutionStarter> providers() {
        return List.copyOf(providers);
    }
}
