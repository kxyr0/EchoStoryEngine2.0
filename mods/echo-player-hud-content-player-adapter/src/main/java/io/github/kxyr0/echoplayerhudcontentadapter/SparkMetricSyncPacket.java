package io.github.kxyr0.echoplayerhudcontentadapter;

import io.github.kxyr0.echoplayer.api.profile.PlayerProfileSnapshot;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricIds;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Compact Spark metric packet produced from Echo Content Player snapshots.
 */
record SparkMetricSyncPacket(int current, int maximum, long revision) {
    static SparkMetricSyncPacket from(PlayerProfileSnapshot snapshot) {
        return new SparkMetricSyncPacket(snapshot.spark().current(), snapshot.spark().maximum(),
                snapshot.repositoryRevision());
    }

    void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(current);
        buffer.writeInt(maximum);
        buffer.writeLong(revision);
    }

    static SparkMetricSyncPacket decode(FriendlyByteBuf buffer) {
        return new SparkMetricSyncPacket(buffer.readInt(), buffer.readInt(), buffer.readLong());
    }

    HudMetricSnapshot toMetric() {
        return HudMetricSnapshot.available(HudMetricIds.SPARK, current, maximum, revision);
    }
}
