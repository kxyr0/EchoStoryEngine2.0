package io.github.kxyr0.echotrigger.forge;

import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.core.runtime.TriggerRuntimeHost;
import io.github.kxyr0.echotrigger.forge.api.authoring.TriggerAuthoringServiceLookupEvent;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceBridge;
import io.github.kxyr0.echotrigger.forge.api.runtime.TriggerServiceLookupEvent;
import io.github.kxyr0.echotrigger.forge.authoring.ForgeTriggerAuthoringService;
import io.github.kxyr0.echotrigger.forge.command.TriggerCommandRegistration;
import io.github.kxyr0.echotrigger.forge.provider.effect.event.EffectEventBridge;
import io.github.kxyr0.echotrigger.forge.provider.equipment.event.EquipmentEventBridge;
import io.github.kxyr0.echotrigger.forge.provider.interaction.block.BlockInteractionEventBridge;
import io.github.kxyr0.echotrigger.forge.provider.inventory.event.InventoryEventBridge;
import io.github.kxyr0.echotrigger.forge.provider.lifecycle.player.PlayerLifecycleEventBridge;
import io.github.kxyr0.echotrigger.forge.provider.progression.event.ProgressionEventBridge;
import io.github.kxyr0.echotrigger.forge.provider.reactive.movement.ReactiveMovementBridge;
import io.github.kxyr0.echotrigger.forge.provider.world.dimension.DimensionEventBridge;
import io.github.kxyr0.echotrigger.forge.provider.world.time.TimeWeatherTickBridge;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.List;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EchoTriggerMod.MOD_ID)
public final class EchoTriggerMod {
    public static final String MOD_ID = "echo_trigger";
    private static final Logger LOGGER = LoggerFactory.getLogger("io.github.kxyr0.echotrigger");

    private final ForgeTriggerServices services;
    private final ForgeTriggerAuthoringService authoringService;

    public EchoTriggerMod() {
        TriggerDefinitionCatalog emptyCatalog = new TriggerDefinitionCatalog(List.of());
        this.services = new ForgeTriggerServices(emptyCatalog, new TriggerRuntimeHost(emptyCatalog));
        this.authoringService = new ForgeTriggerAuthoringService(services, FMLPaths.GAMEDIR.get());
        MinecraftForge.EVENT_BUS.register(new io.github.kxyr0.echotrigger.forge.domain.EventTriggerBridge(services));
        MinecraftForge.EVENT_BUS.addListener(this::onAuthoringServiceLookup);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServiceLookup);
        registerProviders();
    }

    public ForgeTriggerServices services() {
        return services;
    }

    private void registerProviders() {
        MinecraftForge.EVENT_BUS.register(new InventoryEventBridge(services));
        MinecraftForge.EVENT_BUS.register(new EquipmentEventBridge(services));
        MinecraftForge.EVENT_BUS.register(new PlayerLifecycleEventBridge(services));
        MinecraftForge.EVENT_BUS.register(new EffectEventBridge(services));
        MinecraftForge.EVENT_BUS.register(new ProgressionEventBridge(services));
        MinecraftForge.EVENT_BUS.register(new DimensionEventBridge(services));
        MinecraftForge.EVENT_BUS.register(new BlockInteractionEventBridge(services));
        MinecraftForge.EVENT_BUS.register(new TimeWeatherTickBridge(services));
        MinecraftForge.EVENT_BUS.register(new ReactiveMovementBridge(services));
    }

    private void onAuthoringServiceLookup(TriggerAuthoringServiceLookupEvent event) {
        event.provide(authoringService);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TriggerCommandRegistration.register(event.getDispatcher(), services, authoringService);
    }

    private void onServerStarted(ServerStartedEvent event) {
        authoringService.bindResources(event.getServer()::getResourceManager);
        var result = authoringService.reloadCanonicalRoot();
        if (result.successful()) {
            LOGGER.info("Echo Trigger loaded external triggers: definitions={}, sleepSubscriptions={}, revision={}",
                    result.definitions(), result.sleepSubscriptions(), result.afterRevision());
        } else {
            LOGGER.error("Echo Trigger external trigger reload failed: diagnostics={}", result.diagnostics());
        }
    }

    private void onServiceLookup(TriggerServiceLookupEvent event) {
        event.provide(new TriggerServiceBridge(services, services, services, services, services));
    }
}
