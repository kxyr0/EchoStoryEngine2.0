package io.github.kxyr0.echodialogue.forge.api.client;

import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/** Fired when input reveals the current text without leaving its Line. */
public final class EchoDialogueTypewriterRevealEvent extends Event {
    private final DialogueLinePresentationSnapshot presentation;

    public EchoDialogueTypewriterRevealEvent(DialogueLinePresentationSnapshot presentation) {
        this.presentation = Objects.requireNonNull(presentation, "presentation");
    }

    public DialogueLinePresentationSnapshot presentation() {
        return presentation;
    }
}
