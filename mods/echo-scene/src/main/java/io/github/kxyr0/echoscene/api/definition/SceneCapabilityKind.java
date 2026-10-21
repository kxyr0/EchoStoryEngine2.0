package io.github.kxyr0.echoscene.api.definition;

public enum SceneCapabilityKind {
    PLAYER(0),
    CAMERA(1),
    NPC(2),
    OTHER(3);

    private final int order;

    SceneCapabilityKind(int order) {
        this.order = order;
    }

    public int order() {
        return order;
    }
}
