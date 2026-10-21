package io.github.kxyr0.echoitemattachments.core.model;

import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public final class CustodyTransitions {
    private static final Map<ItemCustodyState, EnumSet<ItemCustodyState>> ALLOWED = allowed();

    private CustodyTransitions() {
    }

    public static void requireAllowed(ItemCustodyState from, ItemCustodyState to) {
        if (from == to && from == ItemCustodyState.ATTACHED_TO_SUBJECT) return;
        if (!ALLOWED.getOrDefault(from, EnumSet.noneOf(ItemCustodyState.class)).contains(to)) {
            throw new IllegalStateException("illegal custody transition " + from + " -> " + to);
        }
    }

    private static Map<ItemCustodyState, EnumSet<ItemCustodyState>> allowed() {
        Map<ItemCustodyState, EnumSet<ItemCustodyState>> map =
                new EnumMap<>(ItemCustodyState.class);
        map.put(ItemCustodyState.WORLD_ITEM,
                EnumSet.of(ItemCustodyState.ATTACHED_TO_SUBJECT, ItemCustodyState.RECOVERING));
        map.put(ItemCustodyState.ATTACHED_TO_SUBJECT,
                EnumSet.of(ItemCustodyState.RELEASING, ItemCustodyState.RECOVERING));
        map.put(ItemCustodyState.RELEASING,
                EnumSet.of(ItemCustodyState.WORLD_RELEASED, ItemCustodyState.RECOVERING));
        map.put(ItemCustodyState.RECOVERING, EnumSet.of(ItemCustodyState.WORLD_ITEM,
                ItemCustodyState.ATTACHED_TO_SUBJECT, ItemCustodyState.WORLD_RELEASED));
        map.put(ItemCustodyState.WORLD_RELEASED, EnumSet.noneOf(ItemCustodyState.class));
        return Map.copyOf(map);
    }
}
