package io.github.kxyr0.echotime.forge.config;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TimeConfigWatcher implements Closeable {
    private final Path file;
    private final AtomicBoolean changed = new AtomicBoolean();
    private WatchService service;
    private Thread thread;

    public TimeConfigWatcher(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    }

    public void start() throws IOException {
        if (thread != null) return;
        Files.createDirectories(file.getParent());
        service = FileSystems.getDefault().newWatchService();
        file.getParent().register(service, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        thread = new Thread(this::watch, "Echo-Time-Config-Watcher");
        thread.setDaemon(true);
        thread.start();
    }

    public boolean consumeChange() {
        return changed.getAndSet(false);
    }

    private void watch() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = service.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context() instanceof Path relative
                            && relative.getFileName().equals(file.getFileName())) {
                        changed.set(true);
                    }
                }
                if (!key.reset()) return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException ignored) {
            return;
        }
    }

    @Override
    public void close() throws IOException {
        if (thread != null) thread.interrupt();
        if (service != null) service.close();
        thread = null;
        service = null;
    }
}
