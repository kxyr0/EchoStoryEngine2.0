package io.github.kxyr0.echodialogue.forge.api.client.overlay;

import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/** Requests removal of one matching transient dialogue presentation. */
public final class EchoDialogueTransientClearEvent extends Event {
    private final String presentationId;

    public EchoDialogueTransientClearEvent(String presentationId) {
        this.presentationId = Objects.requireNonNull(presentationId, "presentationId").trim();
        if (this.presentationId.isEmpty()) {
            throw new IllegalArgumentException("presentationId must not be blank");
        }
    }

    public String presentationId() {
        return presentationId;
    }
}
