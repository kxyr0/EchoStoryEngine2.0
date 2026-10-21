package io.github.kxyr0.echodialogue.forge.client.state;

import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.util.Optional;

/** Client-only authoritative dialogue presentation state. */
public final class ClientDialogueState {
    private DialogueOpenMessage current;
    private boolean waitingForServer;

    public boolean open(DialogueOpenMessage message) {
        if (current != null && current.sessionId().equals(message.sessionId())
                && message.revision() <= current.revision()) {
            return false;
        }
        current = message;
        waitingForServer = false;
        return true;
    }

    public boolean close(String sessionId, int revision) {
        if (current != null && current.sessionId().equals(sessionId) && revision >= current.revision()) {
            clear();
            return true;
        }
        return false;
    }

    public Optional<DialogueOpenMessage> current() { return Optional.ofNullable(current); }

    public Optional<DialogueOpenMessage> requestAdvance() {
        if (current == null || waitingForServer || !current.canAdvance()) {
            return Optional.empty();
        }
        waitingForServer = true;
        return Optional.of(current);
    }

    public Optional<ChoiceRequest> requestChoice(String choiceId) {
        if (current == null || waitingForServer || !current.canChoose() || !visibleChoice(choiceId)) {
            return Optional.empty();
        }
        waitingForServer = true;
        return Optional.of(new ChoiceRequest(current.sessionId(), current.revision(), choiceId));
    }

    public Optional<ChoiceRequest> requestChoiceAt(int index) {
        if (current == null || waitingForServer || !current.canChoose()
                || index < 0 || index >= current.choices().size()) {
            return Optional.empty();
        }
        waitingForServer = true;
        String choiceId = current.choices().get(index).id();
        return Optional.of(new ChoiceRequest(current.sessionId(), current.revision(), choiceId));
    }

    public void clear() {
        current = null;
        waitingForServer = false;
    }

    private boolean visibleChoice(String choiceId) {
        return current.choices().stream().anyMatch(choice -> choice.id().equals(choiceId));
    }

    public record ChoiceRequest(String sessionId, int revision, String choiceId) { }
}
