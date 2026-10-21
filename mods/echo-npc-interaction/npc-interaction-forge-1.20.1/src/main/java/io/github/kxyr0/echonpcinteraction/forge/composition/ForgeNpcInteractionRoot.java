package io.github.kxyr0.echonpcinteraction.forge.composition;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpcinteraction.forge.api.event.NpcInteractionExecutionPortRegistrationEvent;
import io.github.kxyr0.echonpcinteraction.forge.api.event.NpcInteractionProviderRegistrationEvent;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProviderRegistry;
import io.github.kxyr0.echonpcinteraction.forge.api.service.EchoNpcInteractionServiceLookupEvent;
import io.github.kxyr0.echonpcinteraction.forge.command.EchoNpcInteractionCommands;
import io.github.kxyr0.echonpcinteraction.forge.config.InteractionServerConfig;
import io.github.kxyr0.echonpcinteraction.forge.provider.VanillaBlockUseProvider;
import io.github.kxyr0.echonpcinteraction.forge.runtime.ServerInteractionRuntime;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcSeatOccupancyRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

public final class ForgeNpcInteractionRoot {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NpcInteractionProviderRegistry providers = new NpcInteractionProviderRegistry();
    private final NpcSeatOccupancyRegistry occupancies = new NpcSeatOccupancyRegistry();
    private final Map<MinecraftServer, ServerInteractionRuntime> runtimes = new LinkedHashMap<>();
    private NpcInteractionExecutionPortRegistrationEvent ports = new NpcInteractionExecutionPortRegistrationEvent();

    public void register(IEventBus modBus, IEventBus forgeBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, InteractionServerConfig.SPEC,
                "echo-npc-interaction-server.toml");
        providers.register(new VanillaBlockUseProvider(occupancies));
        modBus.addListener(this::commonSetup);
        forgeBus.addListener(this::lookup);
        forgeBus.addListener(this::commands);
        forgeBus.addListener(this::tick);
        forgeBus.addListener(this::serverStopping);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new NpcInteractionProviderRegistrationEvent(providers));
            refreshPorts();
        });
    }

    private void lookup(EchoNpcInteractionServiceLookupEvent event) {
        ServerInteractionRuntime runtime = runtime(event.server());
        event.provide(runtime, runtime);
    }

    private void commands(RegisterCommandsEvent event) {
        new EchoNpcInteractionCommands(this::runtime, providers, () -> ports.ports(), occupancies)
                .register(event.getDispatcher());
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            runtime(event.getServer()).tick();
        }
    }

    private void serverStopping(ServerStoppingEvent event) {
        runtimes.remove(event.getServer());
    }

    private ServerInteractionRuntime runtime(MinecraftServer server) {
        if (ports.ports().isEmpty()) {
            refreshPorts();
        }
        return runtimes.computeIfAbsent(server,
                value -> new ServerInteractionRuntime(value, providers, () -> ports.ports(), occupancies));
    }

    private void refreshPorts() {
        NpcInteractionExecutionPortRegistrationEvent registration = new NpcInteractionExecutionPortRegistrationEvent();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(registration);
        ports = registration;
        LOGGER.info("NPC_INTERACTION_PORTS_REGISTERED count={} ports={}", ports.ports().size(),
                ports.ports().stream().map(port -> port.portId()).toList());
    }
}
