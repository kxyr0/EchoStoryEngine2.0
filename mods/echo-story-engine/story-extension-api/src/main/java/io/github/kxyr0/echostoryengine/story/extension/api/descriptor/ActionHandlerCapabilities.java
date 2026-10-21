package io.github.kxyr0.echostoryengine.story.extension.api.descriptor;

/**
 * Declared behavior supported by one external action handler.
 */
public record ActionHandlerCapabilities(
        boolean supportsRecovery,
        boolean supportsCancellation,
        boolean supportsAsyncCompletion,
        boolean idempotentStartRequired) {
    public static ActionHandlerCapabilities basic() {
        return new ActionHandlerCapabilities(false, false, false, true);
    }

    public static ActionHandlerCapabilities recoverableAsync() {
        return new ActionHandlerCapabilities(true, true, true, true);
    }
}
