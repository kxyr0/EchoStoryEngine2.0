package io.github.kxyr0.echodevscenarios.forge;

import io.github.kxyr0.echodevscenarios.forge.network.ScenarioNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoDevScenariosMod.MOD_ID)
public final class EchoDevScenariosMod {
    public static final String MOD_ID = "echo_dev_scenarios";
    private final ForgeScenarioCatalog catalog = new ForgeScenarioCatalog();
    private final ForgeScenarioHost host = new ForgeScenarioHost(catalog);

    public EchoDevScenariosMod() {
        ScenarioNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(catalog.listener());
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        EchoPlayCommands.register(event.getDispatcher(), host);
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) host.tick();
    }
}
