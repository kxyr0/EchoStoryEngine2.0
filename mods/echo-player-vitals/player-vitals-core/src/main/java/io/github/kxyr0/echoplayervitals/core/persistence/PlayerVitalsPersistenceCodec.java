package io.github.kxyr0.echoplayervitals.core.persistence;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Properties;

/**
 * Stable text codec for SavedData payload values.
 */
public final class PlayerVitalsPersistenceCodec {
    public String encode(PlayerVitalsData data) {
        Objects.requireNonNull(data, "data");
        Properties properties = new Properties();
        properties.setProperty("playerId", data.playerId());
        properties.setProperty("fatigueCurrent", Integer.toString(data.fatigueCurrent()));
        properties.setProperty("fatigueMaximum", Integer.toString(data.fatigueMaximum()));
        properties.setProperty("revision", Long.toString(data.revision()));
        try {
            StringWriter writer = new StringWriter();
            properties.store(writer, "echo-player-vitals");
            return writer.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("String writer failed.", exception);
        }
    }

    public PlayerVitalsData decode(String text) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(Objects.requireNonNull(text, "text")));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Vitals payload could not be decoded.", exception);
        }
        return new PlayerVitalsData(properties.getProperty("playerId"),
                Integer.parseInt(properties.getProperty("fatigueCurrent")),
                Integer.parseInt(properties.getProperty("fatigueMaximum")),
                Long.parseLong(properties.getProperty("revision")));
    }
}
