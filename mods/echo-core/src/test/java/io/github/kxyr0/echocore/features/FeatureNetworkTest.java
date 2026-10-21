package io.github.kxyr0.echocore.features;

import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeatureNetworkTest {
    @Test void snapshotRoundTripPreservesOwnerAndBooleanValues() {
        var packet = new FeatureNetwork.Snapshot(UUID.randomUUID(),
                Map.of("thought_space_unlocked", true, "magic_unlocked", false));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(buffer);
            assertEquals(packet, FeatureNetwork.Snapshot.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void oversizedSnapshotIsRejectedBeforeReadingEntries() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUUID(UUID.randomUUID()); buffer.writeVarInt(257);
            assertThrows(IllegalArgumentException.class, () -> FeatureNetwork.Snapshot.decode(buffer));
        } finally { buffer.release(); }
    }
}
