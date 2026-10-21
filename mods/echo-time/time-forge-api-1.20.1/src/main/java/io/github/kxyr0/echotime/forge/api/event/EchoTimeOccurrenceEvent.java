package io.github.kxyr0.echotime.forge.api.event;

import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoTimeOccurrenceEvent extends Event {
    private final MinecraftServer server;
    private final TimeOccurrenceCrossed occurrence;

    public EchoTimeOccurrenceEvent(MinecraftServer server, TimeOccurrenceCrossed occurrence) {
        this.server = Objects.requireNonNull(server, "server");
        this.occurrence = Objects.requireNonNull(occurrence, "occurrence");
    }

    public MinecraftServer server() { return server; }
    public TimeOccurrenceCrossed occurrence() { return occurrence; }
}
