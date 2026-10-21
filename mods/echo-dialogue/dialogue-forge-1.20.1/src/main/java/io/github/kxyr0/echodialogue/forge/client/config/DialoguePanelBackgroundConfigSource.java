package io.github.kxyr0.echodialogue.forge.client.config;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * Loads dialogue panel background settings from a separate story overlay config.
 */
public final class DialoguePanelBackgroundConfigSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Duration SCAN_INTERVAL = Duration.ofMillis(300L);

    private final Path root;
    private final DialoguePanelBackgroundConfigParser parser;
    private DialoguePanelBackgroundConfig current = DialoguePanelBackgroundConfig.defaults();
    private long nextScanAtMs;

    public DialoguePanelBackgroundConfigSource() {
        this(defaultRoot(), "DialoguePanelBackgroundOverlay");
    }

    public DialoguePanelBackgroundConfigSource(String callName) {
        this(defaultRoot(), callName);
    }

    DialoguePanelBackgroundConfigSource(Path root) {
        this(root, "DialoguePanelBackgroundOverlay");
    }

    DialoguePanelBackgroundConfigSource(Path root, String callName) {
        this.root = root.toAbsolutePath().normalize();
        parser = new DialoguePanelBackgroundConfigParser(callName);
    }

    public DialoguePanelBackgroundConfig current() {
        reloadIfDue();
        return current;
    }

    public void refresh() {
        reloadIfDue();
    }

    private void reloadIfDue() {
        long now = System.currentTimeMillis();
        if (now < nextScanAtMs) {
            return;
        }
        nextScanAtMs = now + SCAN_INTERVAL.toMillis();
        current = loadConfig(current);
    }

    private DialoguePanelBackgroundConfig loadConfig(DialoguePanelBackgroundConfig fallback) {
        if (!Files.isDirectory(root)) {
            return DialoguePanelBackgroundConfig.defaults();
        }
        DialoguePanelBackgroundConfig loaded = DialoguePanelBackgroundConfig.defaults();
        try (Stream<Path> stream = Files.find(root, 8, this::isConfigFile)) {
            for (Path file : stream.sorted(Comparator.naturalOrder()).toList()) {
                loaded = parseFile(file, loaded, fallback);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not read dialogue panel background overlay configs: {}", exception.toString());
            return fallback;
        }
        return loaded;
    }

    private boolean isConfigFile(Path path, java.nio.file.attribute.BasicFileAttributes attributes) {
        return attributes.isRegularFile() && path.getFileName().toString().endsWith(".dialogue-overlay.es");
    }

    private DialoguePanelBackgroundConfig parseFile(
            Path file,
            DialoguePanelBackgroundConfig base,
            DialoguePanelBackgroundConfig fallback
    ) {
        try {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            return parser.parse(source, base).orElse(base);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Dialogue panel background overlay rejected: {}: {}", file, exception.toString());
            return fallback;
        }
    }

    private static Path defaultRoot() {
        try {
            return FMLPaths.GAMEDIR.get().resolve("story").resolve("configs");
        } catch (RuntimeException | LinkageError exception) {
            return Path.of("run").resolve("story").resolve("configs");
        }
    }
}
