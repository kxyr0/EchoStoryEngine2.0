package io.github.kxyr0.echoanimation.forge.client.reload;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;

/**
 * Watches external animation authoring files and schedules a debounced reload action on the client thread.
 */
public final class AnimationClientHotReloadWatcher implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Duration POLL_INTERVAL = Duration.ofMillis(250L);
    private static final Duration DEBOUNCE = Duration.ofMillis(350L);
    private static final Duration RECONCILE = Duration.ofSeconds(5L);

    private final Path root;
    private final AnimationClientReloadScheduler reloadScheduler;
    private final Timing timing;
    private final AtomicBoolean running = new AtomicBoolean();
    private final Map<WatchKey, Path> directories = new HashMap<>();
    private WatchService watchService;
    private Thread thread;
    private long lastReconcileMillis;
    private long lastFingerprint;

    public AnimationClientHotReloadWatcher(Path root, Consumer<Runnable> clientExecutor, Runnable reloadAction) {
        this(root, new AnimationClientReloadScheduler(clientExecutor, reloadAction),
                new Timing(POLL_INTERVAL, DEBOUNCE, RECONCILE));
    }

    AnimationClientHotReloadWatcher(Path root, AnimationClientReloadScheduler reloadScheduler, Timing timing) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        this.reloadScheduler = Objects.requireNonNull(reloadScheduler, "reloadScheduler");
        this.timing = Objects.requireNonNull(timing, "timing");
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = new Thread(this::run, "Echo Animation Client Hot Reload");
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        try (WatchService service = FileSystems.getDefault().newWatchService()) {
            watchService = service;
            registerExistingDirectories();
            lastFingerprint = AnimationReloadFileSet.fingerprint(root);
            while (running.get()) {
                pollEvents(service);
                reconcileIfDue();
                reloadIfDebounced();
            }
        } catch (ClosedWatchServiceException ignored) {
            // Shutdown path.
        } catch (IOException exception) {
            LOGGER.warn("Echo Animation client hot reload watcher stopped.", exception);
        } finally {
            running.set(false);
            directories.clear();
        }
    }

    private void pollEvents(WatchService service) {
        WatchKey key;
        try {
            key = service.poll(timing.pollInterval().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running.set(false);
            return;
        }
        if (key == null) {
            return;
        }
        Path directory = directories.get(key);
        boolean changed = false;
        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW || directory == null) {
                changed = true;
                continue;
            }
            Path changedPath = directory.resolve((Path) event.context()).toAbsolutePath().normalize();
            if (Files.isDirectory(changedPath)) {
                registerTree(changedPath);
            }
            changed |= AnimationReloadFileSet.supported(changedPath);
        }
        if (!key.reset()) {
            directories.remove(key);
        }
        if (changed) {
            reloadScheduler.markChanged();
        }
    }

    private void reconcileIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastReconcileMillis < timing.reconcile().toMillis()) {
            return;
        }
        lastReconcileMillis = now;
        registerExistingDirectories();
        long fingerprint = AnimationReloadFileSet.fingerprint(root);
        if (fingerprint != lastFingerprint) {
            lastFingerprint = fingerprint;
            reloadScheduler.markChanged();
        }
    }

    private void reloadIfDebounced() {
        reloadScheduler.reloadIfDebounced(timing.debounce());
    }

    private void registerExistingDirectories() {
        if (Files.isDirectory(root)) {
            registerTree(root);
            return;
        }
        nearestExistingParent().ifPresent(this::registerDirectory);
    }

    private Optional<Path> nearestExistingParent() {
        Path current = root;
        while (current != null) {
            if (Files.isDirectory(current)) {
                return Optional.of(current);
            }
            current = current.getParent();
        }
        return Optional.empty();
    }

    private void registerTree(Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isDirectory).forEach(this::registerDirectory);
        } catch (IOException exception) {
            LOGGER.debug("Echo Animation hot reload could not scan {}", directory, exception);
        }
    }

    private void registerDirectory(Path directory) {
        if (directories.containsValue(directory)) {
            return;
        }
        try {
            WatchKey key = directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            directories.put(key, directory);
        } catch (IOException exception) {
            LOGGER.debug("Echo Animation hot reload could not watch {}", directory, exception);
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException exception) {
                LOGGER.debug("Echo Animation hot reload watcher close failed.", exception);
            }
        }
        if (thread != null) {
            thread.interrupt();
        }
    }

    record Timing(Duration pollInterval, Duration debounce, Duration reconcile) {
    }
}
