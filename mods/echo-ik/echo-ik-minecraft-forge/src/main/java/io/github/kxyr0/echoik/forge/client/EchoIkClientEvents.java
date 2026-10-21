package io.github.kxyr0.echoik.forge.client;

import io.github.kxyr0.echoik.forge.runtime.IkRuntimeState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Client-side Echo IK event wiring.
 */
public final class EchoIkClientEvents {
    private final IkRuntimeState state;
    private final EchoIkPoseOverlay overlay;

    public EchoIkClientEvents(IkRuntimeState state) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.overlay = new EchoIkPoseOverlay(state);
    }

    public void register(IEventBus forgeBus) {
        forgeBus.addListener(overlay::onPoseOverlay);
        forgeBus.addListener(this::onClientTick);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            state.tick();
        }
    }
}
