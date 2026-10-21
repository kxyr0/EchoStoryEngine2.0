package io.github.kxyr0.echodialogue.forge.client.lifecycle;

import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.api.client.DialogueCloseReason;
import io.github.kxyr0.echodialogue.forge.api.client.DialogueLineCue;
import io.github.kxyr0.echodialogue.forge.api.client.DialogueLinePresentationSnapshot;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueLineEnterCommittedEvent;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueLineExitCommittedEvent;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueTypewriterRevealEvent;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

/** Publishes public lifecycle only after client presentation state commits. */
public final class ClientDialogueLifecycle {
    private ClientDialogueLifecycle() {
    }

    public static void transition(Optional<DialogueOpenMessage> previous, DialogueOpenMessage next) {
        previous.filter(ClientDialogueLifecycle::line).ifPresent(message -> exit(message,
                message.sessionId().equals(next.sessionId())
                        ? EchoDialogueLineExitCommittedEvent.Reason.LINE_ADVANCED
                        : EchoDialogueLineExitCommittedEvent.Reason.DIALOGUE_REPLACED));
        if (line(next)) {
            MinecraftForge.EVENT_BUS.post(new EchoDialogueLineEnterCommittedEvent(snapshot(next)));
        }
    }

    public static void close(DialogueOpenMessage previous, DialogueCloseReason reason) {
        if (line(previous)) {
            exit(previous, reason == DialogueCloseReason.COMPLETED
                    ? EchoDialogueLineExitCommittedEvent.Reason.DIALOGUE_COMPLETED
                    : EchoDialogueLineExitCommittedEvent.Reason.DIALOGUE_CANCELLED);
        }
    }

    public static void disconnect(Optional<DialogueOpenMessage> previous) {
        previous.filter(ClientDialogueLifecycle::line).ifPresent(message -> exit(message,
                EchoDialogueLineExitCommittedEvent.Reason.PLAYER_DISCONNECTED));
    }

    public static void reveal(DialogueOpenMessage message) {
        MinecraftForge.EVENT_BUS.post(new EchoDialogueTypewriterRevealEvent(snapshot(message)));
    }

    private static void exit(DialogueOpenMessage message, EchoDialogueLineExitCommittedEvent.Reason reason) {
        MinecraftForge.EVENT_BUS.post(new EchoDialogueLineExitCommittedEvent(snapshot(message), reason));
    }

    private static boolean line(DialogueOpenMessage message) {
        return message.kind() == DialoguePresentationKind.LINE;
    }

    private static DialogueLinePresentationSnapshot snapshot(DialogueOpenMessage message) {
        return new DialogueLinePresentationSnapshot(
                new DialogueSessionId(message.sessionId()),
                DialogueId.parse(message.dialogueId()),
                message.revision(),
                message.speakerId(),
                message.cues().stream().map(cue -> new DialogueLineCue(cue.name(), cue.value())).toList());
    }
}
