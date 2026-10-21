package io.github.kxyr0.echonpcbackpack.core.transition;

import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionType;
import java.util.List;

public final class BackpackMarkerSequence {
    public static final String TAKE = "echo_npc_sophisticated_backpacks:take_from_world";
    public static final String LEFT_HAND = "echo_npc_sophisticated_backpacks:attach_left_hand";
    public static final String RIGHT_HAND = "echo_npc_sophisticated_backpacks:attach_right_hand";
    public static final String BACK = "echo_npc_sophisticated_backpacks:attach_back";
    public static final String DETACH = "echo_npc_sophisticated_backpacks:detach_from_back";
    public static final String PLACE = "echo_npc_sophisticated_backpacks:place_in_world";

    private BackpackMarkerSequence() {
    }

    public static List<String> required(NpcBackpackTransitionType type) {
        return type == NpcBackpackTransitionType.EQUIP ? List.of(TAKE, BACK) : List.of(DETACH, PLACE);
    }

    public static boolean hand(String marker) {
        return LEFT_HAND.equals(marker) || RIGHT_HAND.equals(marker);
    }
}
