package io.github.kxyr0.echodialogue.forge.network.message;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client-to-server request to select one visible choice.
 */
public record DialogueSelectChoiceMessage(String sessionId, int expectedRevision, String choiceId) {
    public DialogueSelectChoiceMessage {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        choiceId = Objects.requireNonNull(choiceId, "choiceId");
    }

    public static void encode(DialogueSelectChoiceMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.sessionId());
        buffer.writeVarInt(message.expectedRevision());
        buffer.writeUtf(message.choiceId());
    }

    public static DialogueSelectChoiceMessage decode(FriendlyByteBuf buffer) {
        return new DialogueSelectChoiceMessage(buffer.readUtf(), buffer.readVarInt(), buffer.readUtf());
    }
}
