package io.github.kxyr0.echonavigation.api.identity;

/**
 * Open provider ID. Core code must not switch over a closed enum of backends.
 */
public record NavigationBackendId(String value) implements Comparable<NavigationBackendId> {
    public static final NavigationBackendId VANILLA = new NavigationBackendId("echo_navigation:vanilla");
    public static final NavigationBackendId ASTAR = new NavigationBackendId("echo_navigation:astar");

    public NavigationBackendId {
        value = NavigationIdSyntax.requireNamespaced(value, "NavigationBackendId");
    }

    @Override
    public int compareTo(NavigationBackendId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
