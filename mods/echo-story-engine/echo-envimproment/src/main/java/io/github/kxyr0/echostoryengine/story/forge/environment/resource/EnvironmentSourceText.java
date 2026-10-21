package io.github.kxyr0.echostoryengine.story.forge.environment.resource;

import java.util.Objects;

public record EnvironmentSourceText(String sourceId, String content) {
    public EnvironmentSourceText {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        content = Objects.requireNonNull(content, "content");
    }
}
