package io.github.kxyr0.echostoryengine.story.extension.forge;

import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Server-scoped event posted once by Echo Story Engine while a host starts.
 */
public final class EchoStoryExtensionRegistrationEvent extends Event {
    private final MinecraftServer server;
    private final ForgeStoryExtensionRegistrar registrar;
    private final StoryExtensionApiVersion engineApiVersion;

    public EchoStoryExtensionRegistrationEvent(
            MinecraftServer server,
            ForgeStoryExtensionRegistrar registrar,
            StoryExtensionApiVersion engineApiVersion) {
        this.server = Objects.requireNonNull(server, "server");
        this.registrar = Objects.requireNonNull(registrar, "registrar");
        this.engineApiVersion = Objects.requireNonNull(engineApiVersion, "engineApiVersion");
    }

    public MinecraftServer server() {
        return server;
    }

    public ForgeStoryExtensionRegistrar registrar() {
        return registrar;
    }

    public StoryExtensionApiVersion engineApiVersion() {
        return engineApiVersion;
    }
}
