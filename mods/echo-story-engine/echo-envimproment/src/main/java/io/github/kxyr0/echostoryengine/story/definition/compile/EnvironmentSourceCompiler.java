package io.github.kxyr0.echostoryengine.story.definition.compile;

import java.time.Instant;

/**
 * Loader-neutral boundary for compiling EnvironmentResource source text.
 */
public interface EnvironmentSourceCompiler {
    EnvironmentCompilationResult compile(EnvironmentSourceInput input, Instant occurredAt);
}
