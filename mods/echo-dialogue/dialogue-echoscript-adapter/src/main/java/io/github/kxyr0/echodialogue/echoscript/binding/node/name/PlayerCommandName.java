package io.github.kxyr0.echodialogue.echoscript.binding.node.name;

import java.util.Map;
import java.util.Optional;

public final class PlayerCommandName {
    private static final Map<String, String> WAIT_TYPES = Map.ofEntries(
            Map.entry("Player.SpawnAt", "echo_player_actor:spawn_at"),
            Map.entry("Player.SetHotbarItem", "echo_player_actor:set_hotbar_item"),
            Map.entry("Player.SelectHotbarSlot", "echo_player_actor:select_hotbar_slot"),
            Map.entry("Player.SitAt", "echo_player_actor:sit_at"),
            Map.entry("Player.Rotate", "echo_player_actor:rotate"),
            Map.entry("Player.LookAt", "echo_player_actor:look_at"),
            Map.entry("Player.Move", "echo_player_actor:move"),
            Map.entry("Player.Stop", "echo_player_actor:stop"),
            Map.entry("Player.Interact", "echo_player_actor:interact"),
            Map.entry("Player.StandUp", "echo_player_actor:stand_up"),
            Map.entry("Player.StopUsingItem", "echo_player_actor:stop_using_item"),
            Map.entry("Player.IsMoving", "echo_player_actor:is_moving"),
            Map.entry("Player.IsSitting", "echo_player_actor:is_sitting"),
            Map.entry("Player.SelectedHotbarSlot", "echo_player_actor:selected_hotbar_slot"),
            Map.entry("Player.IsUsingItem", "echo_player_actor:is_using_item"),
            Map.entry("Player.SetFatigue", "echo_player_vitals:set_fatigue"),
            Map.entry("SetFatigue", "echo_player_vitals:set_fatigue"),
            Map.entry("Player.AddFatigue", "echo_player_vitals:add_fatigue"),
            Map.entry("AddFatigue", "echo_player_vitals:add_fatigue"),
            Map.entry("Player.ReduceFatigue", "echo_player_vitals:reduce_fatigue"),
            Map.entry("ReduceFatigue", "echo_player_vitals:reduce_fatigue"),
            Map.entry("Player.RestoreFatigue", "echo_player_vitals:restore_fatigue"),
            Map.entry("RestoreFatigue", "echo_player_vitals:restore_fatigue"));

    private PlayerCommandName() {
    }

    public static Optional<String> waitType(String rootName) {
        return Optional.ofNullable(WAIT_TYPES.get(rootName));
    }
}
