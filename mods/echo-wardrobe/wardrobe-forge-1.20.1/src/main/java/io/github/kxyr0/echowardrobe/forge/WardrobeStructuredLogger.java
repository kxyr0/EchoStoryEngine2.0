package io.github.kxyr0.echowardrobe.forge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bounded structured logger facade for wardrobe runtime events.
 */
public final class WardrobeStructuredLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoWardrobeMod.MOD_ID);

    public void info(String event, String context, long revision) {
        LOGGER.info("event={} context={} revision={}", event, context, revision);
    }

    public void debug(String event, String context, long revision) {
        LOGGER.debug("event={} context={} revision={}", event, context, revision);
    }

    public void warn(String event, String context, String message) {
        LOGGER.warn("event={} context={} message={}", event, context, message);
    }
}
