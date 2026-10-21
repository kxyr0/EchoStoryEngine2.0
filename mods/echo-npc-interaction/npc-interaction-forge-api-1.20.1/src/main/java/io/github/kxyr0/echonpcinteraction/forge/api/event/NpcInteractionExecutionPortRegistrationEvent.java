package io.github.kxyr0.echonpcinteraction.forge.api.event;

import io.github.kxyr0.echonpcinteraction.forge.api.execution.NpcInteractionExecutionPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class NpcInteractionExecutionPortRegistrationEvent extends Event {
    private final List<NpcInteractionExecutionPort> ports = new ArrayList<>();

    public void register(NpcInteractionExecutionPort port) {
        ports.add(Objects.requireNonNull(port, "port"));
    }

    public List<NpcInteractionExecutionPort> ports() {
        return List.copyOf(ports);
    }
}
