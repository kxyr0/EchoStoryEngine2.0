package io.github.kxyr0.echoplayer.forge.log;

import io.github.kxyr0.echoplayer.forge.EchoContentPlayerMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bounded structured logger facade for the player runtime.
 */
public final class PlayerStructuredLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoContentPlayerMod.MOD_ID);

    public void debug(String event, String playerId, long revision) {
        LOGGER.debug("event={} player={} revision={}", event, playerId, revision);
    }

    public void info(String event, String playerId, long revision) {
        LOGGER.info("event={} player={} revision={}", event, playerId, revision);
    }

    public void warn(String event, String playerId, String message) {
        LOGGER.warn("event={} player={} message={}", event, playerId, message);
    }

    public void error(String event, String playerId, String message) {
        LOGGER.error("event={} player={} message={}", event, playerId, message);
    }
}
