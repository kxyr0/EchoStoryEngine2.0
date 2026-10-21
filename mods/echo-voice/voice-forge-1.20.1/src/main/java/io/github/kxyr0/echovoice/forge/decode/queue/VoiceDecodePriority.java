package io.github.kxyr0.echovoice.forge.decode.queue;

public enum VoiceDecodePriority {
    CURRENT_LINE(0),
    IMMEDIATE_NEXT(1),
    HOT_RELOAD(2),
    PRELOAD(3);

    private final int order;

    VoiceDecodePriority(int order) {
        this.order = order;
    }

    int order() {
        return order;
    }
}
