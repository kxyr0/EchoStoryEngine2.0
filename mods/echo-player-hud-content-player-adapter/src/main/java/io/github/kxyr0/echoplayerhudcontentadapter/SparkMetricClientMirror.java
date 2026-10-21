package io.github.kxyr0.echoplayerhudcontentadapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * Client-local latest Spark metric snapshot.
 */
final class SparkMetricClientMirror {
    static final SparkMetricClientMirror INSTANCE = new SparkMetricClientMirror();
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AtomicReference<HudMetricSnapshot> snapshot = new AtomicReference<>();

    private SparkMetricClientMirror() {
    }

    void apply(SparkMetricSyncPacket packet) {
        snapshot.updateAndGet(current -> current == null
                || packet.revision() >= current.revision().value() ? packet.toMetric() : current);
        LOGGER.info("Echo Player HUD Spark client mirror received current={} maximum={} revision={}",
                packet.current(), packet.maximum(), packet.revision());
    }

    Optional<HudMetricSnapshot> snapshot() {
        return Optional.ofNullable(snapshot.get());
    }
}
