package io.github.kxyr0.echonavigation.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import org.slf4j.Logger;

public final class NavigationStructuredLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    public void diagnostic(NavigationDiagnostic diagnostic) {
        String line = diagnostic.code() + " " + diagnostic.message() + " " + diagnostic.context();
        switch (diagnostic.severity()) {
            case DEBUG -> LOGGER.debug(line);
            case INFO -> LOGGER.info(line);
            case WARN -> LOGGER.warn(line);
            case ERROR -> LOGGER.error(line);
        }
    }

    public void info(String event, String context) {
        LOGGER.info("{} {}", event, context);
    }
}
