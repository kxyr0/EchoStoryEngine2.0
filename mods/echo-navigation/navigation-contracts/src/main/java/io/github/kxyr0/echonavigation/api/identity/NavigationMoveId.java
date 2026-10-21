package io.github.kxyr0.echonavigation.api.identity;

/**
 * Durable movement identity. Script-owned moves use deterministic values.
 */
public record NavigationMoveId(String value) implements Comparable<NavigationMoveId> {
    public NavigationMoveId {
        value = NavigationIdSyntax.requireNamespaced(value, "NavigationMoveId");
    }

    @Override
    public int compareTo(NavigationMoveId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
