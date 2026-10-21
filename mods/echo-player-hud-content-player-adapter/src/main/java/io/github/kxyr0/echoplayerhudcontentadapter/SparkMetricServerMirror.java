package io.github.kxyr0.echoplayerhudcontentadapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Server-side adapter mirror that sends Spark changes without owning Spark storage.
 */
final class SparkMetricServerMirror {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final SparkMetricSyncChannel syncChannel;
    private final ContentPlayerServiceLookup services = new ContentPlayerServiceLookup();
    private final Map<UUID, Long> revisions = new LinkedHashMap<>();

    SparkMetricServerMirror(SparkMetricSyncChannel syncChannel) {
        this.syncChannel = syncChannel;
    }

    void publish(ServerPlayer player) {
        Optional<PlayerProfileSnapshot> profile = services.lookup(player.getServer())
                .flatMap(service -> service.findByAccountUuid(player.getUUID()));
        if (profile.isEmpty()) {
            if (player.tickCount % 100 == 0) {
                LOGGER.warn("Echo Player HUD Spark server mirror has no Echo Content Player profile player={} uuid={}",
                        player.getGameProfile().getName(), player.getUUID());
            }
            return;
        }
        PlayerProfileSnapshot snapshot = profile.orElseThrow();
        if (changed(player.getUUID(), snapshot)) {
            LOGGER.info("Echo Player HUD Spark server mirror sends player={} current={} maximum={} revision={}",
                    player.getGameProfile().getName(), snapshot.spark().current(), snapshot.spark().maximum(),
                    snapshot.repositoryRevision());
            syncChannel.sendTo(player, SparkMetricSyncPacket.from(snapshot));
        }
    }

    private boolean changed(UUID accountUuid, PlayerProfileSnapshot snapshot) {
        long previous = revisions.getOrDefault(accountUuid, -1L);
        if (previous == snapshot.repositoryRevision()) {
            return false;
        }
        revisions.put(accountUuid, snapshot.repositoryRevision());
        return true;
    }
}
