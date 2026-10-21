package io.github.kxyr0.echoitem.forge.reload;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.core.reload.ItemReloadOutcome;
import io.github.kxyr0.echoitem.forge.ForgeItemRuntimeState;
import io.github.kxyr0.echoitem.forge.network.CatalogSyncChannel;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

/**
 * Periodically reconciles external item-pack sources on the server thread.
 */
public final class ItemPackReloadTicker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int RELOAD_INTERVAL_TICKS = 40;
    private final ForgeItemRuntimeState state;
    private final EquippedWeaponRefresher equippedWeapons;
    private final CatalogSyncChannel catalogSync;
    private int ticksUntilReload = RELOAD_INTERVAL_TICKS;

    public ItemPackReloadTicker(ForgeItemRuntimeState state, EquippedWeaponRefresher equippedWeapons,
                                CatalogSyncChannel catalogSync) {
        this.state = Objects.requireNonNull(state, "state");
        this.equippedWeapons = Objects.requireNonNull(equippedWeapons, "equippedWeapons");
        this.catalogSync = Objects.requireNonNull(catalogSync, "catalogSync");
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ticksUntilReload--;
        if (ticksUntilReload > 0) {
            return;
        }
        ticksUntilReload = RELOAD_INTERVAL_TICKS;
        reconcile(event.getServer());
    }

    private void reconcile(MinecraftServer server) {
        ItemReloadOutcome outcome = state.reload();
        int changed = outcome.counters().changedWeaponFiles();
        if (outcome.applied() && changed > 0) {
            equippedWeapons.refresh(server);
            catalogSync.sendToAll(server, state.catalog().snapshot());
        }
        if (changed > 0 || !outcome.applied()) {
            LOGGER.info("Echo Item Pack reload applied={}, changed weapon files={}, diagnostics={}",
                    outcome.applied(), changed, outcome.diagnostics().size());
        }
        outcome.diagnostics().stream()
                .filter(ItemDefinitionDiagnostic::error)
                .limit(5)
                .forEach(diagnostic -> LOGGER.warn("Echo Item Pack reload diagnostic {} at {}: {}",
                        diagnostic.code(), diagnostic.sourcePath(), diagnostic.message()));
    }
}
