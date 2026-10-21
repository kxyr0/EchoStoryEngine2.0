package io.github.kxyr0.echovoice.forge.reload.watch;

import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnostic;
import io.github.kxyr0.echovoice.api.diagnostic.VoiceDiagnosticCode;
import io.github.kxyr0.echovoice.forge.catalog.VoiceAssetPathResolver;
import io.github.kxyr0.echovoice.forge.diagnostic.VoiceLogSink;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

/** Dedicated watcher thread that emits only normalized file events. */
public final class VoiceFileWatcher implements AutoCloseable {
    private final VoiceAssetPathResolver resolver;
    private final VoiceReloadDebouncer debouncer;
    private final VoiceLogSink diagnostics;
    private final WatchService watcher;
    private final Map<WatchKey, Path> directories = new HashMap<>();
    private final Thread thread;
    private volatile boolean closed;

    public VoiceFileWatcher(VoiceAssetPathResolver resolver, VoiceReloadDebouncer debouncer,
            VoiceLogSink diagnostics) throws IOException {
        this.resolver = resolver;
        this.debouncer = debouncer;
        this.diagnostics = diagnostics;
        watcher = FileSystems.getDefault().newWatchService();
        Files.createDirectories(resolver.contentPacksRoot());
        registerTree(resolver.contentPacksRoot());
        thread = new Thread(this::watch, "Echo Voice File Watcher");
        thread.setDaemon(true);
        thread.start();
    }

    private void watch() {
        while (!closed) {
            try {
                WatchKey key = watcher.take();
                Path directory = directories.get(key);
                if (directory != null) {
                    process(directory, key.pollEvents());
                }
                if (!key.reset()) {
                    directories.remove(key);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException exception) {
                warn("Voice watcher ignored malformed filesystem event: " + exception.getMessage());
            }
        }
    }

    private void process(Path directory, Iterable<WatchEvent<?>> events) {
        for (WatchEvent<?> event : events) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }
            Path changed = directory.resolve((Path) event.context()).toAbsolutePath().normalize();
            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
                try {
                    registerTree(changed);
                } catch (IOException exception) {
                    warn("Voice watcher could not register a new directory: " + changed);
                    continue;
                }
            }
            resolver.keyForPath(changed).ifPresent(key -> debouncer.touch(key, System.currentTimeMillis()));
        }
    }

    private void registerTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.filter(Files::isDirectory).forEach(path -> {
                try {
                    WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
                    directories.put(key, path);
                } catch (IOException exception) {
                    warn("Voice watcher could not register directory: " + path);
                }
            });
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        thread.interrupt();
        watcher.close();
    }

    private void warn(String message) {
        diagnostics.accept(VoiceDiagnostic.warning(VoiceDiagnosticCode.VOICE_HOT_RELOAD_FAILED, message, null));
    }
}
