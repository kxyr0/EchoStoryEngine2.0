package io.github.kxyr0.echonpcbackpack.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonavigation.forge.api.agent.NavigationAgentResolverRegistrationEvent;
import io.github.kxyr0.echonpcbackpack.forge.api.NpcBackpackServiceLookupEvent;
import io.github.kxyr0.echonpcbackpack.forge.approach.BackpackNavigationAgentResolver;
import io.github.kxyr0.echonpcbackpack.forge.client.BackpackClientBootstrap;
import io.github.kxyr0.echonpcbackpack.forge.command.BackpackCommands;
import io.github.kxyr0.echonpcbackpack.forge.network.BackpackNetwork;
import io.github.kxyr0.echonpcbackpack.forge.runtime.BackpackRuntime;
import java.io.IOException;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoNpcSophisticatedBackpacksMod.MOD_ID)
public final class EchoNpcSophisticatedBackpacksMod {
    public static final String MOD_ID = "echo_npc_sophisticated_backpacks";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BackpackRuntime runtime = new BackpackRuntime();
    private final BackpackCommands commands = new BackpackCommands(runtime);

    public EchoNpcSophisticatedBackpacksMod() {
        BackpackNetwork.register();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> BackpackClientBootstrap::register);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::playerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::serviceLookup);
        MinecraftForge.EVENT_BUS.addListener(this::registerNavigationResolver);
    }

    private void serverStarted(ServerStartedEvent event) {
        try {
            runtime.start(event.getServer());
            LOGGER.info("event=backpack_runtime_started mod={} version={}", MOD_ID, runtimeVersion());
        } catch (IOException exception) {
            LOGGER.error("event=backpack_runtime_start_failed code=ENPC-SB-JOURNAL-LOAD", exception);
        }
    }

    private String runtimeVersion() {
        return ModList.get().getModContainerById(MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    private void serverStopping(ServerStoppingEvent event) {
        runtime.stop();
        LOGGER.info("event=backpack_runtime_stopped mod={}", MOD_ID);
    }

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) runtime.tick();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        commands.register(event.getDispatcher());
    }

    private void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            runtime.sendFullSnapshots(player);
        }
    }

    private void serviceLookup(NpcBackpackServiceLookupEvent event) {
        event.provide(runtime);
    }

    private void registerNavigationResolver(NavigationAgentResolverRegistrationEvent event) {
        if (!ModList.get().isLoaded("echo_npc_navigation_adapter")) {
            event.registry().register(new BackpackNavigationAgentResolver());
        }
    }
}
