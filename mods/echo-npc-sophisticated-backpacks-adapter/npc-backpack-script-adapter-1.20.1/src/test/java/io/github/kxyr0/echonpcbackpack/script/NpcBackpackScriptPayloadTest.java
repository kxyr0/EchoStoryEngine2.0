package io.github.kxyr0.echonpcbackpack.script;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class NpcBackpackScriptPayloadTest {
    @Test
    void readsHostNormalizedCoordinates() {
        NpcBackpackScriptPayload payload = NpcBackpackScriptPayload.read(Map.of(
                "npc", "jdh:molly", "x", "37", "y", "-3", "z", "100"));

        assertEquals("jdh:molly", payload.actorId());
        assertEquals(37, payload.position().x());
        assertEquals("minecraft:overworld", payload.dimension());
    }

    @Test
    void readsTriggerPositionPayloadAndCreatesStableCommandId() {
        Map<String, String> values = Map.of("arg0", "molly", "arg1", "Position(37,-3,100)");
        NpcBackpackScriptPayload payload = NpcBackpackScriptPayload.read(values);

        assertEquals("jdh:molly", payload.actorId());
        assertEquals(-3, payload.position().y());
        assertEquals(payload.commandId("trigger:1", "equip"), payload.commandId("trigger:1", "equip"));
    }
}
