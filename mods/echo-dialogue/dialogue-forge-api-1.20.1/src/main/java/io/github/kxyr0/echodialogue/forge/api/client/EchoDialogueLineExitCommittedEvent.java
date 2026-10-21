package io.github.kxyr0.echodialogue.forge.api.client;

import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/** Fired before the next committed presentation is entered. */
public final class EchoDialogueLineExitCommittedEvent extends Event {
    private final DialogueLinePresentationSnapshot presentation;
    private final Reason reason;

    public EchoDialogueLineExitCommittedEvent(DialogueLinePresentationSnapshot presentation, Reason reason) {
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public DialogueLinePresentationSnapshot presentation() {
        return presentation;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        LINE_ADVANCED,
        DIALOGUE_COMPLETED,
        DIALOGUE_CANCELLED,
        DIALOGUE_REPLACED,
        PLAYER_DISCONNECTED,
        CLIENT_RESET
    }
}
