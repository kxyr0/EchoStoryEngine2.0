package io.github.kxyr0.echostoryengine.story.forge.host;

/**
 * Server-scoped host lifecycle state.
 */
public enum ForgeServerHostState {
    NOT_STARTED,
    STARTING,
    ACTIVE,
    STOPPING,
    STOPPED,
    FAILED
}
