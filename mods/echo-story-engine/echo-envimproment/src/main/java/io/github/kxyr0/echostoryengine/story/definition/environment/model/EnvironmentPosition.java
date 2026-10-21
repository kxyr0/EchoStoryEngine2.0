package io.github.kxyr0.echostoryengine.story.definition.environment.model;

/**
 * Authored integer block position controlled by an environment resource.
 */
public record EnvironmentPosition(int x, int y, int z) {
    public String asString() {
        return x + " " + y + " " + z;
    }
}
