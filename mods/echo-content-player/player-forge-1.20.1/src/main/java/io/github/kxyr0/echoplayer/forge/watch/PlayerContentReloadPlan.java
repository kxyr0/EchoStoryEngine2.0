package io.github.kxyr0.echoplayer.forge.watch;

/**
 * Immutable file-watcher output consumed on the server thread.
 */
public record PlayerContentReloadPlan(
        int changedTemplateScripts,
        int parserCalls,
        int binderCalls,
        int changedPngFiles,
        int pngValidations,
        int clientReloadSchedules
) {
    public static PlayerContentReloadPlan none() {
        return new PlayerContentReloadPlan(0, 0, 0, 0, 0, 0);
    }
}
