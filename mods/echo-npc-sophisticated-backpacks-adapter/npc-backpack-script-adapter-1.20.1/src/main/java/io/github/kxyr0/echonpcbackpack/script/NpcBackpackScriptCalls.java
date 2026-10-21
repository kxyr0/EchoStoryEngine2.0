package io.github.kxyr0.echonpcbackpack.script;

import java.util.List;

public final class NpcBackpackScriptCalls {
    public static final String EQUIP = "Npc.EquipBackpack";
    public static final String UNEQUIP = "Npc.UnequipBackpack";
    public static final String IS_WEARING = "Npc.IsWearingBackpack";
    public static final String HAS_TRANSITION = "Npc.HasBackpackTransition";
    public static final String IS_AT = "Npc.BackpackIsAt";

    private NpcBackpackScriptCalls() {
    }

    public static List<String> all() {
        return List.of(EQUIP, UNEQUIP, IS_WEARING, HAS_TRANSITION, IS_AT);
    }
}
