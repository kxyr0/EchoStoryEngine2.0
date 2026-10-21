package io.github.kxyr0.echoplayer.forge;

import io.github.kxyr0.echoplayer.forge.api.EchoPlayerServiceLookupEvent;
import io.github.kxyr0.echoplayer.forge.api.appearance.PlayerAppearanceExtensionRegistryBridge;
import io.github.kxyr0.echoplayer.forge.api.appearance.PlayerAppearanceExtensionRegistryEvent;
import io.github.kxyr0.echoplayer.forge.appearance.PlayerAppearanceExtensions;
import io.github.kxyr0.echoplayer.forge.client.PlayerClientNameEvents;
import io.github.kxyr0.echoplayer.forge.command.PlayerCommandRegistration;
import io.github.kxyr0.echoplayer.forge.data.SavedDataPlayerProfileRepository;
import io.github.kxyr0.echoplayer.forge.dialogue.PlayerDialoguePresentationRegistration;
import io.github.kxyr0.echoplayer.forge.identity.PlayerNameEvents;
import io.github.kxyr0.echoplayer.forge.lifecycle.PlayerLoginHooks;
import io.github.kxyr0.echoplayer.forge.log.PlayerStructuredLogger;
import io.github.kxyr0.echoplayer.forge.sync.PlayerProfilePublisher;
import io.github.kxyr0.echoplayer.forge.sync.PlayerProfileSyncChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Forge entrypoint for Echo Content Player.
 */
@Mod(EchoContentPlayerMod.MOD_ID)
public final class EchoContentPlayerMod {
    public static final String MOD_ID = "echo_content_player";
    public static final String MOD_VERSION = "0.1.5";
    private final ForgePlayerRuntimeState state;
    private final PlayerProfileSyncChannel syncChannel;
    private final PlayerLoginHooks loginHooks;
    private final PlayerAppearanceExtensions appearanceExtensions = new PlayerAppearanceExtensions();

    public EchoContentPlayerMod() {
        PlayerStructuredLogger logger = new PlayerStructuredLogger();
        this.syncChannel = new PlayerProfileSyncChannel();
        this.state = ForgePlayerRuntimeState.create(logger, new PlayerProfilePublisher(syncChannel)::publish);
        this.loginHooks = new PlayerLoginHooks(state, syncChannel, logger);
        PlayerAppearanceExtensionRegistryBridge.attach(appearanceExtensions);
        PlayerNameEvents nameEvents = new PlayerNameEvents(state);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServiceLookup);
        MinecraftForge.EVENT_BUS.addListener(this::onAppearanceRegistryLookup);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerStartTracking);
        MinecraftForge.EVENT_BUS.addListener(nameEvents::onNameFormat);
        MinecraftForge.EVENT_BUS.addListener(nameEvents::onTabListNameFormat);
        if (ModList.get().isLoaded("echo_dialogue")) {
            PlayerDialoguePresentationRegistration.register(state.services(), appearanceExtensions, MOD_VERSION);
        }
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> PlayerClientNameEvents::register);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new PlayerCommandRegistration(state.services()).register(event.getDispatcher());
    }

    private void onServiceLookup(EchoPlayerServiceLookupEvent event) {
        state.useRepository(SavedDataPlayerProfileRepository.open(event.server()));
        event.provide(state.services());
    }

    private void onAppearanceRegistryLookup(PlayerAppearanceExtensionRegistryEvent event) {
        event.provide(appearanceExtensions);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        loginHooks.onLogin(event);
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        loginHooks.onLogout(event);
    }

    private void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        loginHooks.onStartTracking(event);
    }
}
