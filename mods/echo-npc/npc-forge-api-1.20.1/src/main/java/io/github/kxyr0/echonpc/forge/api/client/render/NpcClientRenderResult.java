package io.github.kxyr0.echonpc.forge.api.client.render;

/**
 * Explicit outcome of one public NPC render extension invocation.
 */
public enum NpcClientRenderResult {
    NOT_HANDLED,
    RENDERED_OVERLAY,
    REPLACED_DEFAULT_MODEL,
    /** Cancels the complete NPC render, including name, shadow and all remaining layers. */
    CANCELLED,
    FAILED
}
