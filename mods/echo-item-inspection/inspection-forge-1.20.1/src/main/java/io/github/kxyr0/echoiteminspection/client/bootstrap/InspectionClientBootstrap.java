package io.github.kxyr0.echoiteminspection.client.bootstrap;

import io.github.kxyr0.echoiteminspection.client.command.InspectionClientCommands;
import io.github.kxyr0.echoiteminspection.client.input.InventoryInspectionInterceptor;
import io.github.kxyr0.echoiteminspection.client.render.InspectionBackground;
import io.github.kxyr0.echoiteminspection.client.render.WorldInspectionPromptRenderer;
import io.github.kxyr0.echoiteminspection.client.input.WorldInspectionInput;
import io.github.kxyr0.echoiteminspection.client.integration.ipla.IplaInspectionIntegration;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime;
import net.minecraftforge.eventbus.api.EventPriority;
import io.github.kxyr0.echoiteminspection.forge.InspectionRuntime;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class InspectionClientBootstrap {
    private int ticks;

    private InspectionClientBootstrap() { }

    public static void register() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        var bootstrap = new InspectionClientBootstrap();
        modBus.addListener(InspectionBackground::registerShaders);
        modBus.addListener(WorldInspectionInput::registerKeys);
        IplaInspectionIntegration.install();
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(InventoryInspectionInterceptor::onMousePressed);
        bus.addListener(InventoryInspectionInterceptor::onMouseReleased);
        bus.addListener(EventPriority.LOWEST, WorldInspectionInput::onKey);
        bus.addListener(EventPriority.LOWEST, WorldInspectionInput::onMouse);
        bus.addListener(WorldInspectionPromptRenderer::render);
        bus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> WorldInspectionRuntime.clear());
        bus.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> InspectionRuntime.reload());
        bus.addListener((RegisterClientCommandsEvent event) -> InspectionClientCommands.register(event));
        bus.addListener(bootstrap::tick);
        modBus.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener(
                (ResourceManagerReloadListener) manager -> Minecraft.getInstance().execute(
                        () -> InspectionRuntime.resourceReload(manager))));
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().level == null) return;
        if (++ticks < 20) return;
        ticks = 0;
        InspectionRuntime.poll();
    }
}
