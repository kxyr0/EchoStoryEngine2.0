package io.github.kxyr0.echostoryengine.story.persistence.file;

import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.persistence.file.layout.ExecutionFileSet;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshot;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshotCodec;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshotTranslator;
import io.github.kxyr0.echostoryengine.story.persistence.schema.SnapshotDecodeException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Optional;

final class SnapshotFileWriter {
    private final ExecutionSnapshotCodec codec;
    private final ExecutionSnapshotTranslator translator;

    SnapshotFileWriter(ExecutionSnapshotCodec codec, ExecutionSnapshotTranslator translator) {
        this.codec = codec;
        this.translator = translator;
    }

    void write(
            ExecutionFileSet files,
            ExecutionCheckpoint checkpoint,
            Optional<ExecutionSnapshot> current,
            Instant occurredAt) throws IOException {
        Instant createdAt = current.map(snapshot -> Instant.parse(snapshot.createdAt())).orElse(occurredAt);
        String content = codec.encode(translator.fromCheckpoint(checkpoint, createdAt, occurredAt));
        writeTemp(files.temp(), content);
        validateTemp(files.temp());
        if (Files.exists(files.primary())) {
            Files.copy(files.primary(), files.backup(), StandardCopyOption.REPLACE_EXISTING);
        }
        moveTemp(files.temp(), files.primary());
    }

    private void writeTemp(Path temp, String content) throws IOException {
        try (FileChannel channel = FileChannel.open(
                temp,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(content);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private void validateTemp(Path temp) throws IOException {
        try {
            ExecutionSnapshot snapshot = codec.decode(Files.readString(temp, StandardCharsets.UTF_8));
            translator.toCheckpoint(snapshot);
        } catch (SnapshotDecodeException exception) {
            throw new IOException("temporary snapshot did not validate", exception);
        }
    }

    private void moveTemp(Path temp, Path primary) throws IOException {
        try {
            Files.move(temp, primary, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, primary, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
