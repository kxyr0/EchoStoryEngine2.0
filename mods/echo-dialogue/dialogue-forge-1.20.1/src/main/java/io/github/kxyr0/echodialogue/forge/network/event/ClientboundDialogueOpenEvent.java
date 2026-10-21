package io.github.kxyr0.echodialogue.forge.network.event;

import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Client-side event emitted after receiving an open/update packet.
 */
public final class ClientboundDialogueOpenEvent extends Event {
    private final DialogueOpenMessage message;

    public ClientboundDialogueOpenEvent(DialogueOpenMessage message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    public DialogueOpenMessage message() {
        return message;
    }
}
