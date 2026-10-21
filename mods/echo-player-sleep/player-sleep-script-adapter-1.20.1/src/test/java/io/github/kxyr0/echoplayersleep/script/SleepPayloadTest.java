package io.github.kxyr0.echoplayersleep.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echoplayersleep.api.world.DimensionId;
import io.github.kxyr0.echoplayersleep.api.world.Vector3;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SleepPayloadTest {
    @Test
    void destinationAcceptsPackedTarget() {
        SleepPayload payload = new SleepPayload(Map.of(
                "target", "minecraft:overworld;1000000.5;289.0;-1010240.5;180.0;0.0"));

        var destination = payload.destination();

        assertEquals(new DimensionId("minecraft:overworld"), destination.dimension());
        assertEquals(new Vector3(1000000.5, 289.0, -1010240.5), destination.position());
        assertEquals(180.0F, destination.yaw());
        assertEquals(0.0F, destination.pitch());
    }

    @Test
    void destinationRejectsBrokenPackedTarget() {
        SleepPayload payload = new SleepPayload(Map.of("target", "minecraft:overworld;0;64"));

        assertThrows(IllegalArgumentException.class, payload::destination);
    }

    @Test
    void waitTicksAcceptsSeconds() {
        SleepPayload payload = new SleepPayload(Map.of("seconds", "0.25"));

        assertEquals(5L, payload.waitTicks(20L));
    }
}
