package io.github.kxyr0.echocore.client;

import io.github.kxyr0.echocore.api.ChapterDefinition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class ChapterConfiguration {
    private ChapterConfiguration() { }
    public static Map<String, ChapterDefinition> load(Path game) throws IOException {
        Path file = game.resolve("config/echo-core/chapters.properties");
        Properties values = new Properties();
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            values.setProperty("chapter_1.template", "saves/world");
            try (var writer = Files.newBufferedWriter(file)) { values.store(writer, "Chapter IDs map to template world folders; paths are relative to Minecraft or absolute."); }
        } else try (var reader = Files.newBufferedReader(file)) { values.load(reader); }
        Map<String, ChapterDefinition> chapters = new LinkedHashMap<>();
        for (String key : values.stringPropertyNames().stream().sorted().toList()) {
            if (!key.endsWith(".template")) throw new IOException("Unknown chapter config key: " + key);
            String id = key.substring(0, key.length() - ".template".length());
            String template = values.getProperty(key).trim();
            if (template.isEmpty()) throw new IOException("Template path is empty: " + id);
            Path path = Path.of(template);
            try { chapters.put(id, new ChapterDefinition(id, path.isAbsolute() ? path : game.resolve(path))); }
            catch (IllegalArgumentException failure) { throw new IOException("Invalid chapter definition: " + id, failure); }
        }
        return Map.copyOf(chapters);
    }
}
