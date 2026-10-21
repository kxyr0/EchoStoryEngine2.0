package io.github.kxyr0.echodialogue.persistence.file;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Low-level temp/backup/primary file operations.
 */
final class SessionFileStore {
    private final Path root;
    private final SessionFileLayout layout;

    SessionFileStore(Path root) {
        this.root = root;
        this.layout = new SessionFileLayout(root);
    }

    List<Path> primaries() throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.list(root)) {
            return stream.filter(this::snapshotFile)
                    .map(this::primaryForSnapshotFile)
                    .distinct()
                    .sorted()
                    .toList();
        }
    }

    boolean exists(DialogueSessionId sessionId) {
        return Files.exists(layout.primary(sessionId)) || Files.exists(layout.backup(sessionId));
    }

    byte[] read(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    void write(DialogueSessionId sessionId, byte[] bytes) throws IOException {
        Files.createDirectories(root);
        Path temp = layout.temp(sessionId);
        Path primary = layout.primary(sessionId);
        Path backup = layout.backup(sessionId);
        writeTemp(temp, bytes);
        if (Files.exists(primary)) {
            Files.move(primary, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        moveTemp(temp, primary);
        Files.deleteIfExists(temp);
    }

    void delete(DialogueSessionId sessionId) throws IOException {
        Files.deleteIfExists(layout.primary(sessionId));
        Files.deleteIfExists(layout.backup(sessionId));
        Files.deleteIfExists(layout.temp(sessionId));
    }

    Path primary(DialogueSessionId sessionId) {
        return layout.primary(sessionId);
    }

    Path backupForPrimary(Path primary) {
        return layout.backupForPrimary(primary);
    }

    private void writeTemp(Path temp, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(bytes));
            channel.force(true);
        }
    }

    private void moveTemp(Path temp, Path primary) throws IOException {
        try {
            Files.move(temp, primary, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, primary, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean snapshotFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".json") || name.endsWith(".bak");
    }

    private Path primaryForSnapshotFile(Path path) {
        String name = path.getFileName().toString();
        if (name.endsWith(".bak")) {
            return path.resolveSibling(name.replaceFirst("\\.bak$", ".json"));
        }
        return path;
    }
}
