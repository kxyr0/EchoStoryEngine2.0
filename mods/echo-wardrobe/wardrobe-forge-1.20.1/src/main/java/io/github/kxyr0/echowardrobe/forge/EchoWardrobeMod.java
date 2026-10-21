package io.github.kxyr0.echowardrobe.forge;

import io.github.kxyr0.echowardrobe.forge.api.EchoWardrobeServiceLookupEvent;
import io.github.kxyr0.echowardrobe.forge.command.WardrobeCommandRegistration;
import io.github.kxyr0.echowardrobe.forge.content.ForgeWardrobeCatalogLoader;
import io.github.kxyr0.echowardrobe.forge.client.WardrobeClientEvents;
import io.github.kxyr0.echowardrobe.forge.equipment.PlayerWardrobeSynchronizer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge entrypoint for Echo Wardrobe.
 */
@Mod(EchoWardrobeMod.MOD_ID)
public final class EchoWardrobeMod {
    public static final String MOD_ID = "echo_wardrobe";
    public static final String MOD_VERSION = "0.1.24";

    private final ForgeWardrobeRuntimeState state = new ForgeWardrobeRuntimeState();
    private final PlayerWardrobeSynchronizer synchronizer = new PlayerWardrobeSynchronizer(state);
    private final ForgeWardrobeCatalogLoader loader = new ForgeWardrobeCatalogLoader(FMLPaths.GAMEDIR.get());
    private final WardrobeStructuredLogger logger = new WardrobeStructuredLogger();

    public EchoWardrobeMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServiceLookup);
        MinecraftForge.EVENT_BUS.addListener(this::onEquipmentChange);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onPlayerLoggedIn);
        logger.info("wardrobe_loaded", "empty-production-catalog", state.catalog().revision().value());
        DistExecutor.safeRunWhenOn(Dist.CLIENT,
                () -> () -> WardrobeClientEvents.register(state, loader, FMLPaths.GAMEDIR.get()));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        var result = state.acceptCatalog(loader.load(), "register-commands");
        logger.info("wardrobe_catalog_loaded", "register-commands", state.catalog().revision().value());
        result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.error())
                .forEach(diagnostic -> logger.warn("wardrobe_catalog_load_error",
                        diagnostic.source(), diagnostic.message()));
        new WardrobeCommandRegistration(state, loader).register(event.getDispatcher());
    }

    private void onServiceLookup(EchoWardrobeServiceLookupEvent event) {
        event.provide(state.services());
    }

    private void onEquipmentChange(LivingEquipmentChangeEvent event) {
        synchronizer.onEquipmentChange(event);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        synchronizer.onLogin(event);
    }
}
