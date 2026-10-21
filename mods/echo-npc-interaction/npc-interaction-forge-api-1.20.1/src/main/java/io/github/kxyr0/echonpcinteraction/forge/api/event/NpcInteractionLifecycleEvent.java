package io.github.kxyr0.echonpcinteraction.forge.api.event;

import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionStatus;
import java.util.Map;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class NpcInteractionLifecycleEvent extends Event {
    private final NpcInteractionCommandId commandId;
    private final NpcInteractionStatus status;
    private final Map<String, String> context;

    public NpcInteractionLifecycleEvent(
            NpcInteractionCommandId commandId,
            NpcInteractionStatus status,
            Map<String, String> context) {
        this.commandId = Objects.requireNonNull(commandId, "commandId");
        this.status = Objects.requireNonNull(status, "status");
        this.context = Map.copyOf(Objects.requireNonNull(context, "context"));
    }

    public NpcInteractionCommandId commandId() {
        return commandId;
    }

    public NpcInteractionStatus status() {
        return status;
    }

    public Map<String, String> context() {
        return context;
    }
}
