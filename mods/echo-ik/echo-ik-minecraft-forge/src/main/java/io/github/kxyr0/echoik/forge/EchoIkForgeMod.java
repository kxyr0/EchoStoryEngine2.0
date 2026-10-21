package io.github.kxyr0.echoik.forge;

import io.github.kxyr0.echoik.forge.client.EchoIkClientEvents;
import io.github.kxyr0.echoik.forge.command.EchoIkCommands;
import io.github.kxyr0.echoik.forge.network.IkNetworkChannel;
import io.github.kxyr0.echoik.forge.runtime.IkRuntimeState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge entrypoint for the minimal Echo IK runtime integration.
 */
@Mod(EchoIkForgeMod.MOD_ID)
public final class EchoIkForgeMod {
    public static final String MOD_ID = "echo_ik";
    private static final int BLEND_DURATION_TICKS = 10;

    private final IkRuntimeState serverState = new IkRuntimeState(BLEND_DURATION_TICKS);
    private final IkRuntimeState clientState = new IkRuntimeState(BLEND_DURATION_TICKS);
    private final IkNetworkChannel network = new IkNetworkChannel(clientState);

    public EchoIkForgeMod() {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        network.register();
        forgeBus.addListener(this::onRegisterCommands);
        forgeBus.addListener(this::onServerTick);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> new EchoIkClientEvents(clientState).register(forgeBus));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new EchoIkCommands(serverState, network).register(event.getDispatcher());
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            serverState.tick();
        }
    }
}
