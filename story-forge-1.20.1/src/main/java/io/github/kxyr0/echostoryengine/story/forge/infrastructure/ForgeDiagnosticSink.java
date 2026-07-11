package io.github.kxyr0.echostoryengine.story.forge.infrastructure;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends structured core diagnostics into the standard Forge/Minecraft logging pipeline.
 */
public final class ForgeDiagnosticSink implements DiagnosticSink {
    private static final Logger LOGGER = LoggerFactory.getLogger("io.github.kxyr0.echostoryengine.story");

    @Override
    public void accept(StoryDiagnostic diagnostic) {
        String line = "[{}] {} story={} execution={} node={} correlation={} cause={}";
        Object[] arguments = {
                diagnostic.code(),
                diagnostic.message(),
                diagnostic.subject().storyId(),
                diagnostic.subject().executionId(),
                diagnostic.subject().nodeId(),
                diagnostic.subject().correlationId(),
                diagnostic.causeCategory()
        };
        switch (diagnostic.severity()) {
            case TRACE -> LOGGER.trace(line, arguments);
            case DEBUG -> LOGGER.debug(line, arguments);
            case INFO -> LOGGER.info(line, arguments);
            case WARN -> LOGGER.warn(line, arguments);
            case ERROR -> LOGGER.error(line, arguments);
        }
    }
}
