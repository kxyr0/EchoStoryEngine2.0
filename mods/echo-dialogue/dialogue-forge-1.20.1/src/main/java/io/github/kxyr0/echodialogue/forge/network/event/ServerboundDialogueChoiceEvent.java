package io.github.kxyr0.echodialogue.forge.network.event;

import io.github.kxyr0.echodialogue.forge.network.message.DialogueSelectChoiceMessage;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Server-side event emitted after receiving a client choice selection.
 */
public final class ServerboundDialogueChoiceEvent extends Event {
    private final DialogueSelectChoiceMessage message;
    private final ServerPlayer sender;

    public ServerboundDialogueChoiceEvent(DialogueSelectChoiceMessage message, ServerPlayer sender) {
        this.message = Objects.requireNonNull(message, "message");
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public DialogueSelectChoiceMessage message() {
        return message;
    }

    public ServerPlayer sender() {
        return sender;
    }
}
