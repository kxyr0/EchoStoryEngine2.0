package io.github.kxyr0.echotime.forge.api.event;

import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoTimeWaitCompletedEvent extends Event {
    private final MinecraftServer server;
    private final TimeWaitId waitId;

    public EchoTimeWaitCompletedEvent(MinecraftServer server, TimeWaitId waitId) {
        this.server = Objects.requireNonNull(server, "server");
        this.waitId = Objects.requireNonNull(waitId, "waitId");
    }

    public MinecraftServer server() { return server; }
    public TimeWaitId waitId() { return waitId; }
}
