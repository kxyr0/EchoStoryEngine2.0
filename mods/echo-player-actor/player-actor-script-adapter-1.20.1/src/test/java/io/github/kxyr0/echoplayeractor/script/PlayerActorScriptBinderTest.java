package io.github.kxyr0.echoplayeractor.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PlayerActorScriptBinderTest {
    private final PlayerActorScriptBinder binder = new PlayerActorScriptBinder();

    @Test
    void bindsMovePositionalsToPayload() {
        PlayerActorScriptCommand command = binder.bind("Player.Move",
                Map.of("arg0", "10", "arg1", "64", "arg2", "20")).orElseThrow();

        assertEquals(PlayerActorScriptCommands.MOVE, command.waitType());
        assertEquals("x=10;y=64;z=20", command.payload());
        assertTrue(command.blocking());
    }

    @Test
    void bindsOpeningCommandsToCanonicalWaits() {
        PlayerActorScriptCommand spawn = binder.bind("Player.SpawnAt",
                Map.of("arg0", "120.5", "arg1", "65", "arg2", "194.5",
                        "yaw", "180", "pitch", "8")).orElseThrow();
        PlayerActorScriptCommand item = binder.bind("Player.SetHotbarItem",
                Map.of("arg0", "1", "arg1", "minecraft:book", "count", "1")).orElseThrow();
        PlayerActorScriptCommand select = binder.bind("Player.SelectHotbarSlot",
                Map.of("arg0", "1")).orElseThrow();
        PlayerActorScriptCommand sit = binder.bind("Player.SitAt",
                Map.of("arg0", "121", "arg1", "64", "arg2", "194")).orElseThrow();

        assertEquals(PlayerActorScriptCommands.SPAWN_AT, spawn.waitType());
        assertEquals("pitch=8;x=120.5;y=65;yaw=180;z=194.5", spawn.payload());
        assertEquals(PlayerActorScriptCommands.SET_HOTBAR_ITEM, item.waitType());
        assertEquals("count=1;itemId=minecraft:book;slot=1", item.payload());
        assertEquals(PlayerActorScriptCommands.SELECT_HOTBAR_SLOT, select.waitType());
        assertEquals("slot=1", select.payload());
        assertEquals(PlayerActorScriptCommands.SIT_AT, sit.waitType());
        assertEquals("x=121;y=64;z=194", sit.payload());
    }

    @Test
    void bindsPredicateAsNonBlocking() {
        PlayerActorScriptCommand command = binder.bind("Player.SelectedHotbarSlot", Map.of()).orElseThrow();

        assertEquals(PlayerActorScriptCommands.SELECTED_HOTBAR_SLOT, command.waitType());
        assertTrue(!command.blocking());
    }
}
