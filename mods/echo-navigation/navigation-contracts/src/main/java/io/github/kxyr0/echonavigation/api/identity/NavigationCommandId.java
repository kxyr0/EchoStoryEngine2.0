package io.github.kxyr0.echonavigation.api.identity;

/**
 * Durable idempotency key for one authored command.
 */
public record NavigationCommandId(String value) implements Comparable<NavigationCommandId> {
    public NavigationCommandId {
        value = NavigationIdSyntax.requireNamespaced(value, "NavigationCommandId");
    }

    @Override
    public int compareTo(NavigationCommandId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
