package io.github.kxyr0.echodialogue.forge.network.event;

import io.github.kxyr0.echodialogue.forge.network.message.DialogueAdvanceMessage;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Server-side event emitted after receiving a client advance request.
 */
public final class ServerboundDialogueAdvanceEvent extends Event {
    private final DialogueAdvanceMessage message;
    private final ServerPlayer sender;

    public ServerboundDialogueAdvanceEvent(DialogueAdvanceMessage message, ServerPlayer sender) {
        this.message = Objects.requireNonNull(message, "message");
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public DialogueAdvanceMessage message() {
        return message;
    }

    public ServerPlayer sender() {
        return sender;
    }
}
