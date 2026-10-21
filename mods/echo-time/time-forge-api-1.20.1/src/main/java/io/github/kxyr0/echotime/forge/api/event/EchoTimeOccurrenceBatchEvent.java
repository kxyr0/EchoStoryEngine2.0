package io.github.kxyr0.echotime.forge.api.event;

import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoTimeOccurrenceBatchEvent extends Event {
    private final MinecraftServer server;
    private final List<TimeOccurrenceCrossed> occurrences;

    public EchoTimeOccurrenceBatchEvent(MinecraftServer server, List<TimeOccurrenceCrossed> occurrences) {
        this.server = Objects.requireNonNull(server, "server");
        this.occurrences = List.copyOf(Objects.requireNonNull(occurrences, "occurrences"));
        if (this.occurrences.isEmpty()) throw new IllegalArgumentException("Occurrence batch cannot be empty.");
    }

    public MinecraftServer server() { return server; }
    public List<TimeOccurrenceCrossed> occurrences() { return occurrences; }
}

