package io.github.kxyr0.echoitem.forge.api;

/**
 * Public reload summary that does not expose runtime host implementation.
 */
public record EchoItemReloadSummary(
        long revision,
        int changedWeaponFiles,
        int parserCalls,
        int binderCalls,
        int changedTextures,
        int resourceReloads,
        boolean applied
) {
}
