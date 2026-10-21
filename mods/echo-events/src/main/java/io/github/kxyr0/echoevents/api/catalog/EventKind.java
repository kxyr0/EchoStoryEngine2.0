package io.github.kxyr0.echoevents.api.catalog;

import java.util.Arrays;

public enum EventKind {
    DIALOGUE_STARTED("Dialogue.Started", "dialogue/started", false),
    DIALOGUE_CHOICE("Dialogue.Choice", "dialogue/choice", true),
    DIALOGUE_COMPLETED("Dialogue.Completed", "dialogue/completed", true),
    DIALOGUE_CANCELLED("Dialogue.Cancelled", "dialogue/cancelled", true),
    DIALOGUE_FAILED("Dialogue.Failed", "dialogue/failed", true),
    SCENE_STARTED("Scene.Started", "scene/started", false),
    SCENE_COMPLETED("Scene.Completed", "scene/completed", true),
    SCENE_CANCELLED("Scene.Cancelled", "scene/cancelled", true),
    SCENE_FAILED("Scene.Failed", "scene/failed", true),
    CUTSCENE_STARTED("Cutscene.Started", "cutscene/started", false),
    CUTSCENE_COMPLETED("Cutscene.Completed", "cutscene/completed", true),
    CUTSCENE_CANCELLED("Cutscene.Cancelled", "cutscene/cancelled", true),
    OBSERVATION_COMPLETED("Observation.Completed", "observation/completed", true),
    NPC_ARRIVED("Npc.Arrived", "npc/arrived", false),
    NPC_DIED("Npc.Died", "npc/died", true),
    ANIMATION_MARKER("Animation.Marker", "animation/marker", false),
    ANIMATION_COMPLETED("Animation.Completed", "animation/completed", false),
    TELEPORT_STARTED("TeleportMagic.Started", "teleport_magic/started", false),
    TELEPORT_COMMITTED("TeleportMagic.Committed", "teleport_magic/committed", true),
    TELEPORT_COMPLETED("TeleportMagic.Completed", "teleport_magic/completed", true),
    TELEPORT_CANCELLED("TeleportMagic.Cancelled", "teleport_magic/cancelled", true),
    TELEPORT_FAILED("TeleportMagic.Failed", "teleport_magic/failed", true);

    private final String predicate;
    private final String stableId;
    private final boolean durable;

    static {
        var ids = new java.util.HashSet<String>();
        var predicates = new java.util.HashSet<String>();
        for (var kind : values()) {
            if (!ids.add(kind.stableId) || !predicates.add(kind.predicate)) {
                throw new IllegalStateException("Duplicate Echo event kind");
            }
        }
    }

    EventKind(String predicate, String stableId, boolean durable) {
        this.predicate = predicate;
        this.stableId = "echo:" + stableId;
        this.durable = durable;
    }
    public String predicate() { return predicate; }
    public String stableId() { return stableId; }
    public boolean durable() { return durable; }
    public static EventKind fromStableId(String id) {
        return Arrays.stream(values()).filter(value -> value.stableId.equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown event kind: " + id));
    }
}
