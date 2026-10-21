package io.github.kxyr0.echoplayer.core.persistence;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

/**
 * JDK-only persistence codec used by unit tests and lightweight SavedData hosting.
 */
public final class PlayerPersistenceCodec {
    public String encode(PlayerProfileData data) {
        Properties properties = new Properties();
        properties.setProperty("playerId", data.playerId());
        properties.setProperty("accountName", data.accountName());
        properties.setProperty("nameMode", data.nameMode());
        properties.setProperty("customName", data.customName());
        properties.setProperty("sparkCurrent", Integer.toString(data.sparkCurrent()));
        properties.setProperty("sparkMaximum", Integer.toString(data.sparkMaximum()));
        properties.setProperty("skinMode", data.skinMode());
        properties.setProperty("skinId", data.skinId());
        properties.setProperty("skinModel", data.skinModel());
        properties.setProperty("skinFingerprint", data.skinFingerprint());
        properties.setProperty("templateId", data.templateId());
        properties.setProperty("templateFingerprint", data.templateFingerprint());
        properties.setProperty("profileRevision", Long.toString(data.profileRevision()));
        properties.setProperty("repositoryRevision", Long.toString(data.repositoryRevision()));
        properties.setProperty("appearanceRevision", Long.toString(data.appearanceRevision()));
        properties.setProperty("createdAt", data.createdAt());
        properties.setProperty("lastLoginAt", data.lastLoginAt());
        properties.setProperty("lastLogoutAt", data.lastLogoutAt());
        properties.setProperty("firstAccountName", data.firstAccountName());
        properties.setProperty("lastAccountName", data.lastAccountName());
        properties.setProperty("defaultTemplateApplied", Boolean.toString(data.defaultTemplateApplied()));
        return store(properties);
    }

    public PlayerProfileData decode(String text) {
        Properties properties = load(text);
        return new PlayerProfileData(properties.getProperty("playerId"), properties.getProperty("accountName"),
                properties.getProperty("nameMode"), properties.getProperty("customName"),
                integer(properties, "sparkCurrent"), integer(properties, "sparkMaximum"),
                properties.getProperty("skinMode"), properties.getProperty("skinId"),
                properties.getProperty("skinModel"), properties.getProperty("skinFingerprint"),
                properties.getProperty("templateId"), properties.getProperty("templateFingerprint"),
                longValue(properties, "profileRevision"), longValue(properties, "repositoryRevision"),
                longValue(properties, "appearanceRevision"), properties.getProperty("createdAt"),
                properties.getProperty("lastLoginAt"), properties.getProperty("lastLogoutAt"),
                properties.getProperty("firstAccountName"), properties.getProperty("lastAccountName"),
                Boolean.parseBoolean(properties.getProperty("defaultTemplateApplied")));
    }

    private String store(Properties properties) {
        try {
            StringWriter writer = new StringWriter();
            properties.store(writer, "Echo Content Player profile");
            return writer.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("String properties store failed.", ex);
        }
    }

    private Properties load(String text) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(text));
            return properties;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Profile data is not readable.", ex);
        }
    }

    private int integer(Properties properties, String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    private long longValue(Properties properties, String key) {
        return Long.parseLong(properties.getProperty(key));
    }
}
