package io.github.kxyr0.echoplayerhudvitalsadapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricIds;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricProvider;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import io.github.kxyr0.echoplayervitals.api.identity.PlayerVitalsId;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.forge.api.EchoPlayerVitalsServiceLookupEvent;
import io.github.kxyr0.echoplayervitals.forge.api.client.PlayerVitalsClientBridge;
import io.github.kxyr0.echoplayervitals.forge.api.client.PlayerVitalsClientSnapshotView;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

/**
 * HUD metric provider backed by latest Vitals client snapshot.
 */
public final class VitalsFatigueMetricProvider implements HudMetricProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SNAPSHOT_LOG_INTERVAL_TICKS = 100;
    private final HudMetricId metricId;
    private final boolean reserve;
    private int lastSnapshotLogTick = Integer.MIN_VALUE;

    public VitalsFatigueMetricProvider() {
        this(HudMetricIds.STAMINA, true);
    }

    public static VitalsFatigueMetricProvider fatigue() {
        return new VitalsFatigueMetricProvider(HudMetricIds.FATIGUE, false);
    }

    private VitalsFatigueMetricProvider(HudMetricId metricId, boolean reserve) {
        this.metricId = metricId;
        this.reserve = reserve;
    }

    @Override
    public HudMetricId metricId() {
        return metricId;
    }

    @Override
    public HudMetricSnapshot snapshot() {
        Optional<PlayerVitalsSnapshot> clientVitals = clientSnapshot();
        if (clientVitals.isPresent()) {
            HudMetricSnapshot metric = fromVitals(clientVitals.orElseThrow());
            logSnapshot("client_bridge", metric);
            return metric;
        }
        Optional<PlayerVitalsSnapshot> integratedVitals = integratedServerSnapshot();
        if (integratedVitals.isPresent()) {
            HudMetricSnapshot metric = fromVitals(integratedVitals.orElseThrow());
            logSnapshot("integrated_server", metric);
            return metric;
        }
        logUnavailable("no client snapshot and no integrated server snapshot");
        return HudMetricSnapshot.unavailable(metricId);
    }

    private Optional<PlayerVitalsSnapshot> clientSnapshot() {
        Optional<PlayerVitalsClientSnapshotView> view = PlayerVitalsClientBridge.lookup();
        if (view.isEmpty()) {
            logUnavailable("PlayerVitalsClientBridge has no view");
            return Optional.empty();
        }
        Optional<PlayerVitalsSnapshot> snapshot = view.orElseThrow().local();
        if (snapshot.isEmpty()) {
            logUnavailable("PlayerVitalsClientBridge view has no local snapshot");
        }
        return snapshot;
    }

    private Optional<PlayerVitalsSnapshot> integratedServerSnapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            logUnavailable("no local client player");
            return Optional.empty();
        }
        if (minecraft.getSingleplayerServer() == null) {
            logUnavailable("no integrated server");
            return Optional.empty();
        }
        EchoPlayerVitalsServiceLookupEvent event =
                new EchoPlayerVitalsServiceLookupEvent(minecraft.getSingleplayerServer());
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            logUnavailable("Echo Player Vitals service lookup returned 0 providers");
            return Optional.empty();
        }
        PlayerVitalsId playerId = PlayerVitalsId.fromAccountUuid(minecraft.player.getUUID());
        Optional<PlayerVitalsSnapshot> snapshot = event.providers().get(0).find(playerId);
        if (snapshot.isEmpty()) {
            logUnavailable("Echo Player Vitals snapshot not found playerId=" + playerId);
        }
        return snapshot;
    }

    private HudMetricSnapshot fromVitals(PlayerVitalsSnapshot snapshot) {
        if (reserve) {
            return HudMetricSnapshot.available(metricId(), snapshot.fatigue().reserve().current(),
                    snapshot.fatigue().reserve().maximum(), snapshot.revision().value());
        }
        return HudMetricSnapshot.available(metricId(), snapshot.fatigue().fatigue().current(),
                snapshot.fatigue().fatigue().maximum(), snapshot.revision().value());
    }

    private void logSnapshot(String source, HudMetricSnapshot snapshot) {
        if (shouldLog()) {
            LOGGER.info("Echo Player HUD Vitals snapshot metric={} source={} current={} maximum={} normalized={} "
                            + "revision={}", metricId, source, snapshot.current(), snapshot.maximum(),
                    snapshot.normalized(), snapshot.revision().value());
        }
    }

    private void logUnavailable(String reason) {
        if (shouldLog()) {
            LOGGER.warn("Echo Player HUD Vitals snapshot unavailable metric={} reserve={} reason={}",
                    metricId, reserve, reason);
        }
    }

    private boolean shouldLog() {
        Minecraft minecraft = Minecraft.getInstance();
        int tick = minecraft.player == null ? 0 : minecraft.player.tickCount;
        if (tick == lastSnapshotLogTick || tick % SNAPSHOT_LOG_INTERVAL_TICKS != 0) {
            return false;
        }
        lastSnapshotLogTick = tick;
        return true;
    }
}
