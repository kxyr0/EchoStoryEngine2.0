package io.github.kxyr0.echoobservation.forge.network;

import io.github.kxyr0.echoobservation.echoscript.ObservationResourceCompiler;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservationPacketCodecTest {
    @Test
    void snapshotRoundTripKeepsOverlayTextAndThoughtTextSeparate() {
        var resource = new ObservationResourceCompiler().compile(
                "jdh:echo_observation/observations/room.es", """
                        class Room : ObservationResource {
                            Observation Window() {
                                Text("Внешняя реплика.");
                                Thought("Внутренняя мысль.");
                            }
                        }
                        """).resource().orElseThrow();
        var packet = new ObservationPackets.ClientboundSnapshot(7L, 9L, List.of(resource),
                Set.of("jdh:room"), Set.of(), Set.of("jdh:room#window"));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ObservationPacketCodec.writeSnapshot(packet, buffer);
            var decoded = ObservationPacketCodec.readSnapshot(buffer);
            var definition = decoded.resources().get(0).observations().get(0);
            assertEquals("Внешняя реплика.", definition.text().orElseThrow());
            assertEquals("Внутренняя мысль.", definition.thought().orElseThrow());
            assertEquals(packet.completedObservations(), decoded.completedObservations());
        } finally {
            buffer.release();
        }
    }
}
