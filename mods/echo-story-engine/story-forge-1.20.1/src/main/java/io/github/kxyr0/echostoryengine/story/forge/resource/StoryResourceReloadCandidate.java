package io.github.kxyr0.echostoryengine.story.forge.resource;

import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidate;
import java.util.Objects;

/**
 * Combined Forge reload candidate for story and environment resources.
 */
public record StoryResourceReloadCandidate(
        StoryResourceCandidate stories,
        EnvironmentResourceCandidate environments) {
    public StoryResourceReloadCandidate {
        stories = Objects.requireNonNull(stories, "stories");
        environments = Objects.requireNonNull(environments, "environments");
    }
}
