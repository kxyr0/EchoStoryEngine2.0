package io.github.kxyr0.echodialogue.forge.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Server-scoped registration event for dynamic speaker presentation providers.
 */
public final class EchoDialogueSpeakerPresentationRegistrationEvent extends Event {
    private final MinecraftServer server;
    private final List<SpeakerPresentationProviderFactory> factories = new ArrayList<>();
    private boolean open = true;

    public EchoDialogueSpeakerPresentationRegistrationEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void register(SpeakerPresentationProviderFactory factory) {
        if (!open) {
            throw new IllegalStateException("Регистрация presentation provider уже закрыта.");
        }
        factories.add(Objects.requireNonNull(factory, "factory"));
    }

    public List<SpeakerPresentationProviderFactory> factories() {
        return List.copyOf(factories);
    }

    public void closeRegistration() {
        open = false;
    }
}
