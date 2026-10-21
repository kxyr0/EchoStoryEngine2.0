package io.github.kxyr0.echodialogue.forge.network.event;

import io.github.kxyr0.echodialogue.forge.network.message.DialogueCloseMessage;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Client-side event emitted after receiving a close packet.
 */
public final class ClientboundDialogueCloseEvent extends Event {
    private final DialogueCloseMessage message;

    public ClientboundDialogueCloseEvent(DialogueCloseMessage message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    public DialogueCloseMessage message() {
        return message;
    }
}
