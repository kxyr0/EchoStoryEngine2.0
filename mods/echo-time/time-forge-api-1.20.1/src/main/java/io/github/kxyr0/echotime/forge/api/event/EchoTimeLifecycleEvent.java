package io.github.kxyr0.echotime.forge.api.event;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoTimeLifecycleEvent extends Event {
    private final MinecraftServer server;
    private final EchoTimeLifecyclePhase phase;
    private final EchoTimeSnapshot snapshot;

    public EchoTimeLifecycleEvent(MinecraftServer server, EchoTimeLifecyclePhase phase, EchoTimeSnapshot snapshot) {
        this.server = Objects.requireNonNull(server, "server");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public MinecraftServer server() { return server; }
    public EchoTimeLifecyclePhase phase() { return phase; }
    public EchoTimeSnapshot snapshot() { return snapshot; }
}
