package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

/**
 * User-facing failure while validating or applying an environment state.
 */
public final class EnvironmentApplyException extends Exception {
    private static final long serialVersionUID = 1L;

    public EnvironmentApplyException(String message) {
        super(message);
    }
}
