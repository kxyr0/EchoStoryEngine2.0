package io.github.kxyr0.echodialogue.forge.network.message;

import io.github.kxyr0.echodialogue.forge.api.client.DialogueCloseReason;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server-to-client close presentation.
 */
public record DialogueCloseMessage(String sessionId, int revision, DialogueCloseReason reason) {
    public DialogueCloseMessage {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        reason = Objects.requireNonNull(reason, "reason");
    }

    public DialogueCloseMessage(String sessionId, int revision) {
        this(sessionId, revision, DialogueCloseReason.COMPLETED);
    }

    public static void encode(DialogueCloseMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.sessionId());
        buffer.writeVarInt(message.revision());
        buffer.writeEnum(message.reason());
    }

    public static DialogueCloseMessage decode(FriendlyByteBuf buffer) {
        return new DialogueCloseMessage(buffer.readUtf(), buffer.readVarInt(),
                buffer.readEnum(DialogueCloseReason.class));
    }
}
