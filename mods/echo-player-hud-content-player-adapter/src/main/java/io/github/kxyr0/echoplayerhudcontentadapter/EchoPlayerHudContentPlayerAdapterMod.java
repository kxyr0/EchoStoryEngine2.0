package io.github.kxyr0.echoplayerhudcontentadapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayerhud.forge.api.HudMetricProviderRegistrationEvent;
import io.github.kxyr0.echoplayerhud.forge.api.HudMetricProviderRegistrar;
import io.github.kxyr0.echoplayerhud.forge.api.PlayerHudServiceBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Registers Echo Content Player Spark as the HUD Spark metric.
 */
@Mod(EchoPlayerHudContentPlayerAdapterMod.MOD_ID)
public final class EchoPlayerHudContentPlayerAdapterMod {
    public static final String MOD_ID = "echo_player_hud_content_player_adapter";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final SparkMetricSyncChannel syncChannel = new SparkMetricSyncChannel();
    private final SparkMetricServerMirror serverMirror = new SparkMetricServerMirror(syncChannel);
    private boolean hudProviderRegistrationAttempted;
    private int lateHudLookupAttempts;

    public EchoPlayerHudContentPlayerAdapterMod() {
        LOGGER.info("Echo Player HUD Content Player adapter loaded.");
        MinecraftForge.EVENT_BUS.addListener(this::onHudMetricRegistration);
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
    }

    private void onHudMetricRegistration(HudMetricProviderRegistrationEvent event) {
        registerHudProvider(event.registrar(), "registration_event");
        hudProviderRegistrationAttempted = true;
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || hudProviderRegistrationAttempted) {
            return;
        }
        lateHudLookupAttempts++;
        PlayerHudServiceBridge.lookup().ifPresentOrElse(service -> {
            registerHudProvider(service, "client_tick");
            hudProviderRegistrationAttempted = true;
        }, () -> {
            if (lateHudLookupAttempts % 100 == 1) {
                LOGGER.warn("Echo Player HUD Content Player adapter cannot find HUD service bridge; retrying.");
            }
        });
    }

    private void registerHudProvider(HudMetricProviderRegistrar registrar, String source) {
        boolean registered = registrar.register(new SparkMetricProvider());
        LOGGER.info("Echo Player HUD Content Player adapter Spark provider registration source={} registered={}",
                source, registered);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            serverMirror.publish(player);
        }
    }

    private void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && player.tickCount % 20 == 0) {
            serverMirror.publish(player);
        }
    }
}
