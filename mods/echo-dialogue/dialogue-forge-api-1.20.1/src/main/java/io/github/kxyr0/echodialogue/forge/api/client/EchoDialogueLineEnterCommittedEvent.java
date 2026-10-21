package io.github.kxyr0.echodialogue.forge.api.client;

import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/** Fired after the client accepts a new authoritative Line presentation. */
public final class EchoDialogueLineEnterCommittedEvent extends Event {
    private final DialogueLinePresentationSnapshot presentation;

    public EchoDialogueLineEnterCommittedEvent(DialogueLinePresentationSnapshot presentation) {
        this.presentation = Objects.requireNonNull(presentation, "presentation");
    }

    public DialogueLinePresentationSnapshot presentation() {
        return presentation;
    }
}
