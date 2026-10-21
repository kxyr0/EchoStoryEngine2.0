package io.github.kxyr0.echoanimation.api.animation.definition;

/**
 * High-level MAIN phase behavior.
 */
public enum MainMode {
    /** Play MAIN once, then complete or continue into an authored Exit. */
    ONCE,

    /** Repeat MAIN indefinitely until Stop is requested. Intended for idle/walk/run locomotion loops. */
    LOOP_UNTIL_STOP,

    /** Play MAIN once, clamp to its final pose, and hold it indefinitely until Stop is requested. */
    HOLD_UNTIL_STOP
}
