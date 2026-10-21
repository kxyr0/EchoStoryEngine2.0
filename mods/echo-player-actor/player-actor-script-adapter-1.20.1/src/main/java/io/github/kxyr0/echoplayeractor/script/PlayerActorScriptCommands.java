package io.github.kxyr0.echoplayeractor.script;

import java.util.Map;
import java.util.Optional;

public final class PlayerActorScriptCommands {
    public static final String WAIT_PREFIX = "echo_player_actor:";
    public static final String SPAWN_AT = WAIT_PREFIX + "spawn_at";
    public static final String SET_HOTBAR_ITEM = WAIT_PREFIX + "set_hotbar_item";
    public static final String SELECT_HOTBAR_SLOT = WAIT_PREFIX + "select_hotbar_slot";
    public static final String SIT_AT = WAIT_PREFIX + "sit_at";
    public static final String ROTATE = WAIT_PREFIX + "rotate";
    public static final String LOOK_AT = WAIT_PREFIX + "look_at";
    public static final String MOVE = WAIT_PREFIX + "move";
    public static final String STOP = WAIT_PREFIX + "stop";
    public static final String INTERACT = WAIT_PREFIX + "interact";
    public static final String STAND_UP = WAIT_PREFIX + "stand_up";
    public static final String STOP_USING_ITEM = WAIT_PREFIX + "stop_using_item";
    public static final String IS_MOVING = WAIT_PREFIX + "is_moving";
    public static final String IS_SITTING = WAIT_PREFIX + "is_sitting";
    public static final String SELECTED_HOTBAR_SLOT = WAIT_PREFIX + "selected_hotbar_slot";
    public static final String IS_USING_ITEM = WAIT_PREFIX + "is_using_item";
    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("Player.SpawnAt", SPAWN_AT),
            Map.entry("Player.SetHotbarItem", SET_HOTBAR_ITEM),
            Map.entry("Player.SelectHotbarSlot", SELECT_HOTBAR_SLOT),
            Map.entry("Player.SitAt", SIT_AT),
            Map.entry("Player.Rotate", ROTATE),
            Map.entry("Player.LookAt", LOOK_AT),
            Map.entry("Player.Move", MOVE),
            Map.entry("Player.Stop", STOP),
            Map.entry("Player.Interact", INTERACT),
            Map.entry("Player.StandUp", STAND_UP),
            Map.entry("Player.StopUsingItem", STOP_USING_ITEM),
            Map.entry("Player.IsMoving", IS_MOVING),
            Map.entry("Player.IsSitting", IS_SITTING),
            Map.entry("Player.SelectedHotbarSlot", SELECTED_HOTBAR_SLOT),
            Map.entry("Player.IsUsingItem", IS_USING_ITEM));

    private PlayerActorScriptCommands() {
    }

    public static Optional<String> waitType(String scriptName) {
        return Optional.ofNullable(NAMES.get(scriptName));
    }

    public static boolean predicate(String waitType) {
        return IS_MOVING.equals(waitType) || IS_SITTING.equals(waitType)
                || SELECTED_HOTBAR_SLOT.equals(waitType) || IS_USING_ITEM.equals(waitType);
    }
}
