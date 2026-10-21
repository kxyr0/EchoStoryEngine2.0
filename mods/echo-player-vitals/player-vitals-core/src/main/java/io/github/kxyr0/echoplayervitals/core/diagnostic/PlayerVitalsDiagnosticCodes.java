package io.github.kxyr0.echoplayervitals.core.diagnostic;

/**
 * Stable diagnostic codes emitted by Vitals core and Forge boundaries.
 */
public final class PlayerVitalsDiagnosticCodes {
    public static final String PROFILE_NOT_FOUND = "PLAYER_VITALS_PROFILE_NOT_FOUND";
    public static final String FATIGUE_INVALID = "PLAYER_VITALS_FATIGUE_INVALID";
    public static final String STAGE_AMBIGUOUS = "PLAYER_VITALS_FATIGUE_STAGE_AMBIGUOUS";
    public static final String STAGE_MISSING = "PLAYER_VITALS_FATIGUE_STAGE_MISSING";
    public static final String MOVEMENT_CONFLICT = "PLAYER_VITALS_MOVEMENT_MODIFIER_CONFLICT";
    public static final String HUNGER_DISABLE_FAILED = "PLAYER_VITALS_HUNGER_DISABLE_FAILED";
    public static final String PERSISTENCE_FAILED = "PLAYER_VITALS_PERSISTENCE_FAILED";
    public static final String REVISION_STALE = "PLAYER_VITALS_REVISION_STALE";
    public static final String MUTATION_CONFLICT = "PLAYER_VITALS_MUTATION_IDEMPOTENCY_CONFLICT";

    private PlayerVitalsDiagnosticCodes() {
    }
}
