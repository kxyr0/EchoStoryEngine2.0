package io.github.kxyr0.echotimehud.forge.config;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echotimehud.api.HudAnchor;
import io.github.kxyr0.echotimehud.api.HudConfig;
import io.github.kxyr0.echotimehud.api.HudScreenPolicy;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class HudConfigLoader implements Closeable {
    public static final String RELATIVE_PATH = "story/configs/jdh/echo_time_hud/hud.es";
    private static final String TEMPLATE = "/data/jdh/echo_time_hud/config/hud.es";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path path;
    private final HudConfigBinder binder = new HudConfigBinder();
    private final AtomicBoolean changed = new AtomicBoolean();
    private volatile HudConfig current = defaults(0L);
    private volatile List<HudConfigDiagnostic> diagnostics = List.of();
    private volatile boolean lastLoadSuccessful;
    private long revision;
    private int debounceTicks = -1;
    private WatchService watcher;
    private Thread watchThread;

    public HudConfigLoader(Path gameDirectory) { path = gameDirectory.resolve(RELATIVE_PATH); }
    public static HudConfigLoader gameDirectory() { return new HudConfigLoader(FMLPaths.GAMEDIR.get()); }

    public void start() {
        try {
            seedIfMissing();
            reload();
            watcher = FileSystems.getDefault().newWatchService();
            path.getParent().register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            watchThread = new Thread(this::watch, "Echo-Time-HUD-Config-Watcher");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException exception) {
            diagnostics = List.of(diagnostic("HUD_CONFIG_WATCH_FAILED", exception.getMessage()));
            LOGGER.error("Echo Time HUD config initialization failed path={}", path, exception);
        }
    }

    public void clientTick() {
        if (changed.getAndSet(false)) debounceTicks = 4;
        if (debounceTicks > 0) debounceTicks--;
        else if (debounceTicks == 0) { debounceTicks = -1; reload(); }
    }

    public synchronized boolean reload() {
        long candidateRevision = revision + 1L;
        if (!Files.isRegularFile(path)) return reject(List.of(diagnostic(
                "HUD_CONFIG_MISSING", "hud.es is missing; last-known-good retained.")));
        try {
            HudConfigResult result = binder.bind(Files.readString(path, StandardCharsets.UTF_8),
                    path.toString(), candidateRevision);
            diagnostics = result.diagnostics();
            Optional<HudConfig> next = result.config();
            if (next.isEmpty()) return reject(diagnostics);
            current = next.orElseThrow();
            revision = candidateRevision;
            lastLoadSuccessful = true;
            LOGGER.info("Echo Time HUD config applied revision={} path={}", revision, path);
            return true;
        } catch (IOException exception) {
            return reject(List.of(diagnostic("HUD_CONFIG_READ_FAILED", exception.getMessage())));
        }
    }

    private boolean reject(List<HudConfigDiagnostic> values) {
        diagnostics = List.copyOf(values);
        lastLoadSuccessful = false;
        values.forEach(value -> LOGGER.error("Echo Time HUD config rejected code={} message={} source={}",
                value.code(), value.message(), value.source()));
        return false;
    }

    private void seedIfMissing() throws IOException {
        if (Files.isRegularFile(path)) return;
        Files.createDirectories(path.getParent());
        try (InputStream input = HudConfigLoader.class.getResourceAsStream(TEMPLATE)) {
            if (input == null) throw new IOException("Packaged hud.es template is missing.");
            Files.copy(input, path);
        }
    }

    private void watch() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                var key = watcher.take();
                key.pollEvents().stream().filter(event -> event.context() instanceof Path)
                        .map(event -> (Path) event.context())
                        .filter(relative -> relative.getFileName().equals(path.getFileName()))
                        .findAny().ifPresent(value -> changed.set(true));
                if (!key.reset()) return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException ignored) { }
    }

    public HudConfig current() { return current; }
    public List<HudConfigDiagnostic> diagnostics() { return diagnostics; }
    public boolean lastLoadSuccessful() { return lastLoadSuccessful; }
    public Path path() { return path; }

    @Override public void close() throws IOException {
        if (watchThread != null) watchThread.interrupt();
        if (watcher != null) watcher.close();
        watchThread = null; watcher = null;
    }

    private HudConfigDiagnostic diagnostic(String code, String message) {
        return new HudConfigDiagnostic(code, message, path.toString(), Optional.empty());
    }

    private static HudConfig defaults(long revision) {
        return new HudConfig("jdh:main_time_hud", true, HudAnchor.TOP_RIGHT, -24, 24, 1920, 1080,
                12, 1, "HH:mm", false, true, false, 1, true, true,
                HudScreenPolicy.SHOW_DURING_GAME_SCREENS, revision);
    }
}

