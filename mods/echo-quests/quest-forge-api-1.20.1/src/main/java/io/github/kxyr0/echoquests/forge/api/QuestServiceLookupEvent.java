package io.github.kxyr0.echoquests.forge.api;

import io.github.kxyr0.echoquests.api.runtime.QuestService;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class QuestServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private QuestService service;

    public QuestServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() { return server; }
    public Optional<QuestService> service() { return Optional.ofNullable(service); }

    public void provide(QuestService value) {
        if (service != null && service != value) throw new IllegalStateException("Multiple Echo Quest services provided.");
        service = Objects.requireNonNull(value, "value");
    }
}
