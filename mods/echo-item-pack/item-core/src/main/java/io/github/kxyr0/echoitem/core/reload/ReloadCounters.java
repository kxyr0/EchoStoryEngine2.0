package io.github.kxyr0.echoitem.core.reload;

/**
 * Quantitative reload evidence used by tests and commands.
 */
public record ReloadCounters(
        int changedWeaponFiles,
        int parserCalls,
        int binderCalls,
        int unchangedParserCalls,
        int unchangedBinderCalls,
        int changedTextures,
        int pngValidations,
        int changedModels,
        int modelValidations,
        int resourceReloads
) {
    public static ReloadCounters weapon(int changed) {
        return new ReloadCounters(changed, changed, changed, 0, 0, 0, 0, 0, 0, 0);
    }

    public static ReloadCounters texture(int changed, int reloads) {
        return new ReloadCounters(0, 0, 0, 0, 0, changed, changed, 0, 0, reloads);
    }

    public static ReloadCounters model(int changed, int reloads) {
        return new ReloadCounters(0, 0, 0, 0, 0, 0, 0, changed, changed, reloads);
    }
}
