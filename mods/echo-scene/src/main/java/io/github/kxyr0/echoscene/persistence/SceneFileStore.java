package io.github.kxyr0.echoscene.persistence;

import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SceneFileStore {
    private final Path root;
    private final SceneSnapshotCodec codec = new SceneSnapshotCodec();
    public SceneFileStore(Path root) { this.root = root.toAbsolutePath().normalize(); }
    public void save(SavedScene scene) {
        Path path = path(scene.checkpoint().executionId());
        Path temporary = path.resolveSibling(path.getFileName() + ".writing");
        try {
            Files.createDirectories(root);
            try (var channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                var output = new DataOutputStream(Channels.newOutputStream(channel));
                codec.write(output, scene);
                output.flush();
                channel.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
    public Optional<SavedScene> load(SceneExecutionId id) {
        Path path = path(id);
        if (!Files.exists(path)) return Optional.empty();
        var scene = read(path);
        if (!scene.checkpoint().executionId().equals(id)) throw new IllegalStateException("Scene identity mismatch");
        return Optional.of(scene);
    }
    public List<SavedScene> loadAll() {
        if (!Files.isDirectory(root)) return List.of();
        try (var paths = Files.list(root)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".scene-state"))
                    .sorted().map(this::read).toList();
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
    private SavedScene read(Path path) {
        try (var input = new DataInputStream(Files.newInputStream(path))) {
            if (Files.size(path) > 16 * 1024 * 1024) throw new IOException("Oversized Scene checkpoint");
            var result = codec.read(input);
            if (input.read() != -1) throw new IOException("Trailing Scene checkpoint data");
            return result;
        } catch (IOException failure) { throw new UncheckedIOException(failure); }
    }
    private Path path(SceneExecutionId id) {
        return root.resolve(UUID.nameUUIDFromBytes(id.value().getBytes(StandardCharsets.UTF_8)) + ".scene-state");
    }
}
