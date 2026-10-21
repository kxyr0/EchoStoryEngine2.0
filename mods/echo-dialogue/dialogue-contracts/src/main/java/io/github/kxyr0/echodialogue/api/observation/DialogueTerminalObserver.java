package io.github.kxyr0.echodialogue.api.observation;

/**
 * Callback invoked when a session reaches a durable terminal state.
 */
@FunctionalInterface
public interface DialogueTerminalObserver {
    void onTerminal(DialogueTerminalEvent event);
}
