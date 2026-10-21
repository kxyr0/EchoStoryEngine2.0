package io.github.kxyr0.echoplayerhudvitalsadapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayerhud.forge.api.HudMetricProviderRegistrar;
import io.github.kxyr0.echoplayerhud.forge.api.HudMetricProviderRegistrationEvent;
import io.github.kxyr0.echoplayerhud.forge.api.PlayerHudServiceBridge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Registers Echo Player Vitals fatigue metrics for Echo Player HUD.
 */
@Mod(EchoPlayerHudVitalsAdapterMod.MOD_ID)
public final class EchoPlayerHudVitalsAdapterMod {
    public static final String MOD_ID = "echo_player_hud_vitals_adapter";
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean hudProviderRegistrationAttempted;
    private int lateHudLookupAttempts;

    public EchoPlayerHudVitalsAdapterMod() {
        LOGGER.info("Echo Player HUD Vitals adapter loaded.");
        MinecraftForge.EVENT_BUS.addListener(this::onHudMetricRegistration);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onHudMetricRegistration(HudMetricProviderRegistrationEvent event) {
        registerHudProviders(event.registrar(), "registration_event");
        hudProviderRegistrationAttempted = true;
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || hudProviderRegistrationAttempted) {
            return;
        }
        lateHudLookupAttempts++;
        PlayerHudServiceBridge.lookup().ifPresentOrElse(service -> {
            registerHudProviders(service, "client_tick");
            hudProviderRegistrationAttempted = true;
        }, () -> {
            if (lateHudLookupAttempts % 100 == 1) {
                LOGGER.warn("Echo Player HUD Vitals adapter cannot find HUD service bridge; retrying.");
            }
        });
    }

    private void registerHudProviders(HudMetricProviderRegistrar registrar, String source) {
        boolean reserveRegistered = registrar.register(new VitalsFatigueMetricProvider());
        boolean rawRegistered = registrar.register(VitalsFatigueMetricProvider.fatigue());
        LOGGER.info("Echo Player HUD Vitals adapter provider registration source={} reserveRegistered={} "
                + "rawRegistered={}", source, reserveRegistered, rawRegistered);
    }
}
