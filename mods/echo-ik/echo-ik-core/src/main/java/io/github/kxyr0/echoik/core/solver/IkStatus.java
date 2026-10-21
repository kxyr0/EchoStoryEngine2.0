package io.github.kxyr0.echoik.core.solver;

/**
 * Stable, machine-readable outcome of an IK solve.
 */
public enum IkStatus {
    REACHED,
    CLAMPED_MAX,
    CLAMPED_MIN,
    INVALID
}
