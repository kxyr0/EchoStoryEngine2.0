package io.github.kxyr0.echodialogue.api.observation;

import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import java.util.Objects;

/**
 * Public terminal notification for a durable dialogue session.
 */
public record DialogueTerminalEvent(DialogueSessionView session) {
    public DialogueTerminalEvent {
        session = Objects.requireNonNull(session, "session");
    }
}
