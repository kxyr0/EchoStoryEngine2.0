package io.github.kxyr0.echostorynpcadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostorynpcadapter.action.ActionPayloadDecoder;
import io.github.kxyr0.echostorynpcadapter.action.NpcActionTypes;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionPayloadDecoderTest {
    private final ActionPayloadDecoder decoder = new ActionPayloadDecoder();

    @Test
    void canonicalActionTypesAreExactAndMoveIsAbsent() {
        assertEquals("echo_npc:spawn", NpcActionTypes.SPAWN.asString());
        assertEquals("echo_npc:spawn_at", NpcActionTypes.SPAWN_AT.asString());
        assertEquals("echo_npc:teleport", NpcActionTypes.TELEPORT.asString());
        assertEquals("echo_npc:despawn", NpcActionTypes.DESPAWN.asString());
        assertEquals("echo_npc:sleep_at", NpcActionTypes.SLEEP_AT.asString());
        assertEquals("echo_npc:wake_up", NpcActionTypes.WAKE_UP.asString());
        assertTrue(NpcActionTypes.ALL.stream().noneMatch(id -> id.asString().equals("echo_npc:move")));
    }

    @Test
    void validSpawnArgumentsDecode() {
        var spawn = decoder.spawn(Map.of(
                "actorId", "sample:john",
                "definitionId", "sample:father",
                "worldId", "minecraft:overworld",
                "x", "1.5",
                "y", "64",
                "z", "-2.25",
                "yaw", "90",
                "pitch", "10"));
        assertEquals("sample:john", spawn.actorId().value());
        assertEquals("sample:father", spawn.definitionId().value());
        assertEquals(1.5D, spawn.position().x());
        assertEquals(90.0F, spawn.rotation().yaw());
    }

    @Test
    void validSpawnAtSleepAtAndWakeUpArgumentsDecode() {
        var spawnAt = decoder.spawnAt(Map.of(
                "npcId", "sample:sasha",
                "worldId", "minecraft:overworld",
                "x", "1.5",
                "y", "64",
                "z", "-2.25",
                "yaw", "90",
                "pitch", "0"));
        var sleepAt = decoder.sleepAt(Map.of(
                "npcId", "sample:sasha",
                "worldId", "minecraft:overworld",
                "bedX", "10",
                "bedY", "64",
                "bedZ", "12",
                "expectedBlock", "minecraft:red_bed"));
        var sleepAtAnyBed = decoder.sleepAt(Map.of(
                "npcId", "sample:sasha",
                "worldId", "minecraft:overworld",
                "bedX", "10",
                "bedY", "64",
                "bedZ", "12"));
        var wakeUp = decoder.wakeUp(Map.of("npcId", "sample:sasha"));
        assertEquals("sample:sasha", spawnAt.actorId().value());
        assertEquals("sample:sasha", spawnAt.definitionId().value());
        assertEquals(10, sleepAt.bedPosition().x());
        assertEquals("minecraft:red_bed", sleepAt.expectedBlock().value());
        assertEquals("echo_npc:any_bed", sleepAtAnyBed.expectedBlock().value());
        assertEquals("sample:sasha", wakeUp.actorId().value());
    }

    @Test
    void invalidSpawnArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> decoder.spawn(Map.of("actorId", "sample:john")));
        assertThrows(IllegalArgumentException.class, () -> decoder.spawn(spawnPayload("worldId", "bad")));
        assertThrows(IllegalArgumentException.class, () -> decoder.spawn(spawnPayload("x", "NaN")));
        assertThrows(IllegalArgumentException.class, () -> decoder.spawn(spawnPayload("z", "Infinity")));
        assertThrows(IllegalArgumentException.class, () -> decoder.spawn(spawnPayload("pitch", "120")));
    }

    @Test
    void validTeleportAndDespawnArgumentsDecode() {
        var teleport = decoder.teleport(Map.of(
                "actorId", "sample:john",
                "worldId", "minecraft:overworld",
                "x", "0",
                "y", "70",
                "z", "3",
                "yaw", "0",
                "pitch", "0"));
        var despawn = decoder.despawn(Map.of("actorId", "sample:john"));
        assertEquals("sample:john", teleport.actorId().value());
        assertEquals("sample:john", despawn.actorId().value());
    }

    @Test
    void unknownArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> decoder.despawn(Map.of(
                "actorId", "sample:john",
                "entityUuid", "00000000-0000-0000-0000-000000000000")));
    }

    private Map<String, String> spawnPayload(String key, String value) {
        java.util.HashMap<String, String> payload = new java.util.HashMap<>();
        payload.put("actorId", "sample:john");
        payload.put("definitionId", "sample:father");
        payload.put("worldId", "minecraft:overworld");
        payload.put("x", "1");
        payload.put("y", "64");
        payload.put("z", "2");
        payload.put("yaw", "0");
        payload.put("pitch", "0");
        payload.put(key, value);
        return payload;
    }
}
