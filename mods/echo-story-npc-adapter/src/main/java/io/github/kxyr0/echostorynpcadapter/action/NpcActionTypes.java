package io.github.kxyr0.echostorynpcadapter.action;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.List;

/**
 * Canonical Story action IDs exposed by this adapter.
 */
public final class NpcActionTypes {
    public static final NamespacedId SPAWN = NamespacedId.of("echo_npc:spawn");
    public static final NamespacedId SPAWN_AT = NamespacedId.of("echo_npc:spawn_at");
    public static final NamespacedId TELEPORT = NamespacedId.of("echo_npc:teleport");
    public static final NamespacedId DESPAWN = NamespacedId.of("echo_npc:despawn");
    public static final NamespacedId SLEEP_AT = NamespacedId.of("echo_npc:sleep_at");
    public static final NamespacedId WAKE_UP = NamespacedId.of("echo_npc:wake_up");
    public static final List<NamespacedId> ALL = List.of(SPAWN, SPAWN_AT, TELEPORT, DESPAWN, SLEEP_AT, WAKE_UP);

    private NpcActionTypes() {
    }
}
