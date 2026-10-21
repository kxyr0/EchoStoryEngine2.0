package io.github.kxyr0.echoteleportmagic.forge;

import io.github.kxyr0.echoteleportmagic.api.*;
import io.github.kxyr0.echoteleportmagic.forge.network.TeleportVisualPacket;
import io.github.kxyr0.echoteleportmagic.forge.persistence.TeleportReceiptStore;
import io.github.kxyr0.echoteleportmagic.runtime.TeleportPort.ReceiptStage;
import io.netty.buffer.Unpooled;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TeleportCodecTest {
    @TempDir Path directory;
    @Test void everyVisualControlPacketRoundTrips() {
        for (var kind : TeleportVisualPacket.Kind.values()) {
            var packet = new TeleportVisualPacket(UUID.randomUUID(), UUID.randomUUID(),
                    new TeleportAnchor(1, 64, 2, 33, .6f, 1.17f), 1200, -77373, kind, true, false, 1.95);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try { packet.encode(buffer); assertEquals(packet, TeleportVisualPacket.decode(buffer)); assertEquals(0, buffer.readableBytes()); }
            finally { buffer.release(); }
        }
    }
    @Test void diskReceiptSurvivesNewStoreAndRejectsChangedDestination() {
        var id = TeleportExecutionId.random();
        var e = new TeleportExecution(id, UUID.randomUUID(), new TeleportAnchor(0, 64, 0, 0, .6f, 1.8f),
                new TeleportDestination("jdh:door", "minecraft:overworld", 12, 64, 1, 0, 0),
                0, 3, TeleportState.COMMITTED, true, false, "scene", "test", 1);
        var store = new TeleportReceiptStore(directory);
        store.write(e, ReceiptStage.AFTER_COMMIT);
        assertEquals(ReceiptStage.AFTER_COMMIT, new TeleportReceiptStore(directory).read(id));
        store.validate(e);
        var changed = new TeleportExecution(id, e.subject(), e.source(), new TeleportDestination("jdh:other", "minecraft:overworld", 15, 64, 1, 0, 0),
                0, 3, e.state(), true, false, "scene", "test", 1);
        assertThrows(IllegalStateException.class, () -> store.validate(changed));
        assertTrue(java.nio.file.Files.exists(directory.resolve(id.value() + ".receipt")));
    }
}
