package io.github.kxyr0.echodialogue.forge.network.message;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client-to-server request to advance an active dialogue session.
 */
public record DialogueAdvanceMessage(String sessionId, int expectedRevision) {
    public DialogueAdvanceMessage {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
    }

    public static void encode(DialogueAdvanceMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.sessionId());
        buffer.writeVarInt(message.expectedRevision());
    }

    public static DialogueAdvanceMessage decode(FriendlyByteBuf buffer) {
        return new DialogueAdvanceMessage(buffer.readUtf(), buffer.readVarInt());
    }
}
