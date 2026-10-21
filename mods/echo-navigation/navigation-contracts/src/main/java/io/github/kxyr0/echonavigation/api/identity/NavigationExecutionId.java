package io.github.kxyr0.echonavigation.api.identity;

/**
 * Durable script or admin execution identity that requested movement.
 */
public record NavigationExecutionId(String value) {
    public NavigationExecutionId {
        value = NavigationIdSyntax.requireNamespaced(value, "NavigationExecutionId");
    }

    @Override
    public String toString() {
        return value;
    }
}
