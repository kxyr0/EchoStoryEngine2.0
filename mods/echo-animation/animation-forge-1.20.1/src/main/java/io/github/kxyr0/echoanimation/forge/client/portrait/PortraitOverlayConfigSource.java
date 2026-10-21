package io.github.kxyr0.echoanimation.forge.client.portrait;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * Loads portrait overlay configs from story/configs without coupling to a story module.
 */
public final class PortraitOverlayConfigSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Duration SCAN_INTERVAL = Duration.ofMillis(300L);

    private final Path root;
    private final PortraitOverlayConfigParser parser = new PortraitOverlayConfigParser();
    private Map<String, PortraitOverlayConfig> configs = Map.of();
    private long nextScanAtMs;

    public PortraitOverlayConfigSource() {
        this(FMLPaths.GAMEDIR.get().resolve("story").resolve("configs"));
    }

    PortraitOverlayConfigSource(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public PortraitOverlayConfig config(String visualId) {
        reloadIfDue();
        return configs.getOrDefault(visualId, PortraitOverlayConfig.defaultConfig());
    }

    private void reloadIfDue() {
        long now = System.currentTimeMillis();
        if (now < nextScanAtMs) {
            return;
        }
        nextScanAtMs = now + SCAN_INTERVAL.toMillis();
        configs = loadConfigs(configs);
    }

    private Map<String, PortraitOverlayConfig> loadConfigs(Map<String, PortraitOverlayConfig> fallback) {
        if (!Files.isDirectory(root)) {
            return Map.of();
        }
        Map<String, PortraitOverlayConfig> loaded = new HashMap<>();
        try (Stream<Path> stream = Files.find(root, 8, this::isConfigFile)) {
            for (Path file : stream.toList()) {
                if (!parseFile(file, loaded)) {
                    return fallback;
                }
            }
        } catch (IOException exception) {
            LOGGER.warn("Не удалось прочитать portrait overlay configs: {}", exception.toString());
            return fallback;
        }
        return Map.copyOf(loaded);
    }

    private boolean isConfigFile(Path path, java.nio.file.attribute.BasicFileAttributes attributes) {
        return attributes.isRegularFile() && path.getFileName().toString().endsWith(".portrait-overlay.es");
    }

    private boolean parseFile(Path file, Map<String, PortraitOverlayConfig> loaded) {
        try {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            parser.parse(source).ifPresent(entry -> loaded.put(entry.visualId(), entry.config()));
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Portrait overlay config отклонён: {}: {}", file, exception.toString());
            return false;
        }
    }
}
