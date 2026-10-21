package io.github.kxyr0.echonavigation.api.identity;

/**
 * Durable owner of a movement, such as a DialogueSession or StoryExecution.
 */
public record NavigationOwnerId(String value) {
    public NavigationOwnerId {
        value = NavigationIdSyntax.requireNamespaced(value, "NavigationOwnerId");
    }

    @Override
    public String toString() {
        return value;
    }
}
