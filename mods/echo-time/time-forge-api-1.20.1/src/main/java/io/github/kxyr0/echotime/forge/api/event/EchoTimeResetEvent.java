package io.github.kxyr0.echotime.forge.api.event;

import io.github.kxyr0.echotime.api.value.EchoInstant;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Published after the explicit {@code /echo_time reset} command applied its configured bootstrap instant.
 */
public final class EchoTimeResetEvent extends Event {
    private final MinecraftServer server;
    private final EchoInstant instant;

    public EchoTimeResetEvent(MinecraftServer server, EchoInstant instant) {
        this.server = Objects.requireNonNull(server, "server");
        this.instant = Objects.requireNonNull(instant, "instant");
    }

    public MinecraftServer server() { return server; }
    public EchoInstant instant() { return instant; }
}
