package io.github.kxyr0.echostoryengine.story.core.port;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;

/**
 * Port used by core to publish structured diagnostics without depending on a logging framework.
 */
@FunctionalInterface
public interface DiagnosticSink {
    void accept(StoryDiagnostic diagnostic);
}
