package io.github.kxyr0.echoscene.api.model;

public record SceneRevision(long value) {
    public SceneRevision {
        if (value < 1) {
            throw new IllegalArgumentException("Scene revision must be positive.");
        }
    }
}
