package io.github.kxyr0.echobackgroundnpc.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalog;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalogHost;
import io.github.kxyr0.echobackgroundnpc.forge.api.service.BackgroundNpcServiceLookupEvent;
import io.github.kxyr0.echobackgroundnpc.forge.client.BackgroundClientSetup;
import io.github.kxyr0.echobackgroundnpc.forge.command.BackgroundNpcCommands;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntities;
import io.github.kxyr0.echobackgroundnpc.forge.persistence.BackgroundNpcSavedData;
import io.github.kxyr0.echobackgroundnpc.forge.network.BackgroundNpcNetwork;
import io.github.kxyr0.echobackgroundnpc.forge.resource.BackgroundResourceReloadListener;
import io.github.kxyr0.echobackgroundnpc.forge.runtime.ForgeBackgroundPopulation;
import io.github.kxyr0.echobackgroundnpc.forge.runtime.ForgeBackgroundService;
import io.github.kxyr0.echobackgroundnpc.quest.EchoQuestAmbientQuestDispatcher;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceAccess;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeLifecycleEvent;
import io.github.kxyr0.echotime.forge.api.event.EchoTimeOccurrenceBatchEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EchoBackgroundNpcMod.MOD_ID)
public final class EchoBackgroundNpcMod {
    public static final String MOD_ID = "echo_background_npc";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BackgroundCatalogHost catalogs = new BackgroundCatalogHost();
    private ForgeBackgroundPopulation population;
    private ForgeBackgroundService service;
    private BackgroundNpcSavedData savedData;

    public EchoBackgroundNpcMod() {
        BackgroundNpcNetwork.register();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BackgroundNpcEntities.REGISTER.register(modBus);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> modBus.addListener(BackgroundClientSetup::register));
        MinecraftForge.EVENT_BUS.addListener(this::reloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::serviceLookup);
        MinecraftForge.EVENT_BUS.addListener(this::timeOccurrences);
        MinecraftForge.EVENT_BUS.addListener(this::timeLifecycle);
    }

    private void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BackgroundResourceReloadListener(catalogs,
                this::acceptCatalog, LOGGER));
    }
    private void serverStarted(ServerStartedEvent event) { ensure(event.getServer()); }
    private void serverStopping(ServerStoppingEvent event) {
        if (population != null) {
            savedData.capture(population.agents());
            population.close();
        }
        population = null;
        service = null;
        savedData = null;
    }
    private void serverTick(TickEvent.ServerTickEvent event) {
        if (population != null && event.phase == TickEvent.Phase.END) {
            population.tick(event.getServer().overworld().getGameTime());
        }
    }
    private void commands(RegisterCommandsEvent event) {
        new BackgroundNpcCommands(() -> service).register(event.getDispatcher());
    }
    private void serviceLookup(BackgroundNpcServiceLookupEvent event) {
        ensure(event.server());
        event.provide(service);
    }
    private void timeOccurrences(EchoTimeOccurrenceBatchEvent event) {
        if (population != null) {
            population.occurrence(event);
            if (savedData != null) savedData.capture(population.agents());
        }
    }
    private void timeLifecycle(EchoTimeLifecycleEvent event) {
        if (population != null) population.lifecycle(event);
    }

    private void acceptCatalog(BackgroundCatalog catalog) {
        if (population != null) population.replaceCatalog(catalog, currentMinute(population.server()));
    }
    private void ensure(MinecraftServer server) {
        if (population != null) return;
        population = new ForgeBackgroundPopulation(server, catalogs.current(), currentMinute(server),
                new EchoQuestAmbientQuestDispatcher(), LOGGER);
        savedData = BackgroundNpcSavedData.load(server);
        savedData.restore(population.agents());
        service = new ForgeBackgroundService(server, population);
    }
    private int currentMinute(MinecraftServer server) {
        return EchoTimeServiceAccess.find(server).map(value -> value.query().current().instant().timeOfDay())
                .map(value -> value.hour() * 60 + value.minute()).orElse(0);
    }
}
