package io.github.kxyr0.echoconcentration.forge.client;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ConcentrationInputGuard {
    private final ConcentrationClientRuntime runtime;

    public ConcentrationInputGuard(ConcentrationClientRuntime runtime) {
        this.runtime = runtime;
    }

    @SubscribeEvent
    public void opening(ScreenEvent.Opening event) {
        if (runtime.snapshot().active() && event.getNewScreen() instanceof InventoryScreen) {
            event.setCanceled(true);
        }
    }
}
