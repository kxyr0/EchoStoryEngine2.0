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
 * Loads client dialogue overlay style from story/configs without story-module coupling.
 */
public final class DialogueOverlayStyleConfigSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Duration SCAN_INTERVAL = Duration.ofMillis(300L);

    private final Path root;
    private final DialogueOverlayStyleConfigParser parser = new DialogueOverlayStyleConfigParser();
    private DialogueOverlayStyleConfig current = DialogueOverlayStyleConfig.defaults();
    private long nextScanAtMs;

    public DialogueOverlayStyleConfigSource() {
        this(defaultRoot());
    }

    DialogueOverlayStyleConfigSource(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public DialogueOverlayStyleConfig current() {
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

    private DialogueOverlayStyleConfig loadConfig(DialogueOverlayStyleConfig fallback) {
        if (!Files.isDirectory(root)) {
            return DialogueOverlayStyleConfig.defaults();
        }
        DialogueOverlayStyleConfig loaded = DialogueOverlayStyleConfig.defaults();
        try (Stream<Path> stream = Files.find(root, 8, this::isConfigFile)) {
            for (Path file : stream.sorted(Comparator.naturalOrder()).toList()) {
                loaded = parseFile(file, loaded, fallback);
            }
        } catch (IOException exception) {
            LOGGER.warn("Не удалось прочитать dialogue overlay configs: {}", exception.toString());
            return fallback;
        }
        return loaded;
    }

    private boolean isConfigFile(Path path, java.nio.file.attribute.BasicFileAttributes attributes) {
        return attributes.isRegularFile() && path.getFileName().toString().endsWith(".dialogue-overlay.es");
    }

    private DialogueOverlayStyleConfig parseFile(
            Path file,
            DialogueOverlayStyleConfig base,
            DialogueOverlayStyleConfig fallback
    ) {
        try {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            return parser.parse(source, base).orElse(base);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Dialogue overlay config отклонён: {}: {}", file, exception.toString());
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
