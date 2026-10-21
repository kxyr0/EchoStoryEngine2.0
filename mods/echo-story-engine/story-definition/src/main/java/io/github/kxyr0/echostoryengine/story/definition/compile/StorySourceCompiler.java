package io.github.kxyr0.echostoryengine.story.definition.compile;

import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.time.Instant;

/**
 * Replaceable Story-owned source compiler contract.
 */
public interface StorySourceCompiler {
    /**
     * Compiles one in-memory source document into the existing Story runtime model when valid.
     */
    StoryCompilationResult compile(StorySourceInput input, Instant occurredAt);
}
