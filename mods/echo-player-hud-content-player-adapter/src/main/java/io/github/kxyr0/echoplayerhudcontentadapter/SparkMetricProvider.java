package io.github.kxyr0.echoplayerhudcontentadapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayer.forge.api.EchoPlayerServiceLookupEvent;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricIds;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricProvider;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

/**
 * HUD metric provider backed by adapter-synchronized Content Player Spark.
 */
public final class SparkMetricProvider implements HudMetricProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SNAPSHOT_LOG_INTERVAL_TICKS = 100;
    private int lastSnapshotLogTick = Integer.MIN_VALUE;

    @Override
    public HudMetricId metricId() {
        return HudMetricIds.SPARK;
    }

    @Override
    public HudMetricSnapshot snapshot() {
        Optional<HudMetricSnapshot> clientSnapshot = SparkMetricClientMirror.INSTANCE.snapshot();
        if (clientSnapshot.isPresent()) {
            logSnapshot("client_mirror", clientSnapshot.orElseThrow());
            return clientSnapshot.orElseThrow();
        }
        Optional<HudMetricSnapshot> integratedSnapshot = integratedServerSnapshot();
        if (integratedSnapshot.isPresent()) {
            logSnapshot("integrated_server", integratedSnapshot.orElseThrow());
            return integratedSnapshot.orElseThrow();
        }
        logUnavailable("no client mirror snapshot and no integrated server profile");
        return HudMetricSnapshot.unavailable(metricId());
    }

    private Optional<HudMetricSnapshot> integratedServerSnapshot() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            logUnavailable("no local client player");
            return Optional.empty();
        }
        if (minecraft.getSingleplayerServer() == null) {
            logUnavailable("no integrated server");
            return Optional.empty();
        }
        EchoPlayerServiceLookupEvent event = new EchoPlayerServiceLookupEvent(minecraft.getSingleplayerServer());
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().isEmpty()) {
            logUnavailable("Echo Content Player service lookup returned 0 providers");
            return Optional.empty();
        }
        EchoPlayerId playerId = EchoPlayerId.fromAccountUuid(minecraft.player.getUUID());
        Optional<PlayerProfileSnapshot> profile = event.providers().get(0).find(playerId);
        if (profile.isEmpty()) {
            logUnavailable("Echo Content Player profile not found playerId=" + playerId);
        }
        return profile.map(this::fromProfile);
    }

    private HudMetricSnapshot fromProfile(PlayerProfileSnapshot snapshot) {
        return HudMetricSnapshot.available(metricId(), snapshot.spark().current(),
                snapshot.spark().maximum(), snapshot.repositoryRevision());
    }

    private void logSnapshot(String source, HudMetricSnapshot snapshot) {
        if (shouldLog()) {
            LOGGER.info("Echo Player HUD Spark snapshot source={} current={} maximum={} normalized={} revision={}",
                    source, snapshot.current(), snapshot.maximum(), snapshot.normalized(),
                    snapshot.revision().value());
        }
    }

    private void logUnavailable(String reason) {
        if (shouldLog()) {
            LOGGER.warn("Echo Player HUD Spark snapshot unavailable: {}", reason);
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
