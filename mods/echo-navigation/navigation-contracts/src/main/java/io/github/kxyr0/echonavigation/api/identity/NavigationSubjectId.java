package io.github.kxyr0.echonavigation.api.identity;

/**
 * Durable subject identity resolved by an adapter, for example one NPC actor.
 */
public record NavigationSubjectId(String value) implements Comparable<NavigationSubjectId> {
    public NavigationSubjectId {
        value = NavigationIdSyntax.requireNamespaced(value, "NavigationSubjectId");
    }

    @Override
    public int compareTo(NavigationSubjectId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
