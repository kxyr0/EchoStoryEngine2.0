package io.github.kxyr0.echostoryengine.story.persistence.recovery;

import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import java.util.Optional;

/**
 * Lookup boundary for compiled story definitions used during recovery.
 */
@FunctionalInterface
public interface CompiledStoryCatalog {
    Optional<StoryDefinition> find(StoryId storyId);
}
