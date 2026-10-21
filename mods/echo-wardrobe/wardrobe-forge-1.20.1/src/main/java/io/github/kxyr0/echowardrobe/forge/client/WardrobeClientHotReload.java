package io.github.kxyr0.echowardrobe.forge.client;

import io.github.kxyr0.echowardrobe.forge.ForgeWardrobeRuntimeState;
import io.github.kxyr0.echowardrobe.forge.WardrobeStructuredLogger;
import io.github.kxyr0.echowardrobe.forge.content.ForgeWardrobeCatalogLoader;
import io.github.kxyr0.echowardrobe.forge.content.ForgeWardrobeScriptStamp;
import io.github.kxyr0.echowardrobe.forge.equipment.ForgeEquipmentSnapshots;
import java.nio.file.Path;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

final class WardrobeClientHotReload {
    private static final int POLL_INTERVAL_TICKS = 20;

    private final ForgeWardrobeRuntimeState state;
    private final ForgeWardrobeCatalogLoader loader;
    private final ForgeWardrobeScriptStamp stamp;
    private final WardrobeStructuredLogger logger = new WardrobeStructuredLogger();
    private int ticks;
    private long lastStamp;

    private WardrobeClientHotReload(ForgeWardrobeRuntimeState state, ForgeWardrobeCatalogLoader loader,
            Path gameDir) {
        this.state = Objects.requireNonNull(state, "state");
        this.loader = Objects.requireNonNull(loader, "loader");
        this.stamp = new ForgeWardrobeScriptStamp(Objects.requireNonNull(gameDir, "gameDir"));
        this.lastStamp = stamp.current();
    }

    static void register(ForgeWardrobeRuntimeState state, ForgeWardrobeCatalogLoader loader, Path gameDir) {
        MinecraftForge.EVENT_BUS.addListener(new WardrobeClientHotReload(state, loader, gameDir)::onClientTick);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().level == null) {
            return;
        }
        ticks++;
        if (ticks < POLL_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        long currentStamp = stamp.current();
        if (currentStamp == lastStamp) {
            return;
        }
        lastStamp = currentStamp;
        reload();
    }

    private void reload() {
        var result = state.acceptCatalog(loader.load(), "client-hot-reload");
        if (result.successful()) {
            refreshLocalPlayer();
            logger.info("wardrobe_catalog_hot_reloaded", "client", state.catalog().revision().value());
            return;
        }
        result.diagnostics().stream()
                .filter(diagnostic -> diagnostic.error())
                .forEach(diagnostic -> logger.warn("wardrobe_catalog_hot_reload_error",
                        diagnostic.source(), diagnostic.message()));
    }

    private void refreshLocalPlayer() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            state.observe(player.getUUID(), ForgeEquipmentSnapshots.currentStatic(player));
        }
    }
}
