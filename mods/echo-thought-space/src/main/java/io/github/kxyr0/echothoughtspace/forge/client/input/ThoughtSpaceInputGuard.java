package io.github.kxyr0.echothoughtspace.forge.client.input;

import io.github.kxyr0.echothoughtspace.forge.client.ThoughtSpaceRuntime;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ThoughtSpaceInputGuard {
    private final ThoughtSpaceRuntime runtime;

    public ThoughtSpaceInputGuard(ThoughtSpaceRuntime runtime) { this.runtime = runtime; }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (runtime.captureActive()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void scroll(InputEvent.MouseScrollingEvent event) {
        if (runtime.captureActive()) event.setCanceled(true);
    }
}

