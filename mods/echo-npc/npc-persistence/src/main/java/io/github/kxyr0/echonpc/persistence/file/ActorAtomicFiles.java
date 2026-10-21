package io.github.kxyr0.echonpc.persistence.file;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.persistence.schema.ActorSaveCodec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Atomic sibling temp/backup file operations for one actor record.
 */
final class ActorAtomicFiles {
    private final ActorFileLayout layout;
    private final ActorSaveCodec codec;

    ActorAtomicFiles(ActorFileLayout layout, ActorSaveCodec codec) {
        this.layout = java.util.Objects.requireNonNull(layout, "layout");
        this.codec = java.util.Objects.requireNonNull(codec, "codec");
    }

    void write(ActorId actorId, String json) throws IOException {
        Files.createDirectories(layout.root());
        Path primary = layout.primary(actorId);
        Path temp = layout.temp(actorId);
        Files.deleteIfExists(temp);
        writeTemp(temp, json);
        codec.decode(Files.readString(temp, StandardCharsets.UTF_8));
        preserveBackup(primary, layout.backup(actorId));
        replace(temp, primary);
        Files.deleteIfExists(temp);
    }

    void remove(ActorId actorId) throws IOException {
        Files.deleteIfExists(layout.primary(actorId));
        Files.deleteIfExists(layout.backup(actorId));
        Files.deleteIfExists(layout.temp(actorId));
    }

    private void writeTemp(Path temp, String json) throws IOException {
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
            channel.force(true);
        }
    }

    private void preserveBackup(Path primary, Path backup) throws IOException {
        if (!Files.isRegularFile(primary) || !primaryDecodes(primary)) {
            return;
        }
        Files.copy(primary, backup, StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean primaryDecodes(Path primary) throws IOException {
        try {
            codec.decode(Files.readString(primary, StandardCharsets.UTF_8));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void replace(Path temp, Path primary) throws IOException {
        try {
            Files.move(temp, primary, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, primary, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
