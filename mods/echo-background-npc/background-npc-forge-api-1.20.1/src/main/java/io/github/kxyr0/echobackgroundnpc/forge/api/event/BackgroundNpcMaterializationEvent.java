package io.github.kxyr0.echobackgroundnpc.forge.api.event;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.runtime.MaterializationState;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class BackgroundNpcMaterializationEvent extends Event {
    private final BackgroundNpcId id;
    private final MaterializationState state;
    public BackgroundNpcMaterializationEvent(BackgroundNpcId id, MaterializationState state) {
        this.id = Objects.requireNonNull(id, "id");
        this.state = Objects.requireNonNull(state, "state");
    }
    public BackgroundNpcId id() { return id; }
    public MaterializationState state() { return state; }
}
