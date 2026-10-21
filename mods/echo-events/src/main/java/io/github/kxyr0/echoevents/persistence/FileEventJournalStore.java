package io.github.kxyr0.echoevents.persistence;

import io.github.kxyr0.echoevents.core.journal.EventJournalImage;
import io.github.kxyr0.echoevents.core.journal.EventJournalStore;
import io.github.kxyr0.echoevents.persistence.codec.EventJournalCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class FileEventJournalStore implements EventJournalStore {
    private final Path path;
    private final EventJournalCodec codec = new EventJournalCodec();

    public FileEventJournalStore(Path path) { this.path = path.toAbsolutePath().normalize(); }
    public EventJournalImage load() {
        if (!Files.exists(path)) return EventJournalImage.empty();
        try {
            if (Files.size(path) > 256L * 1024 * 1024) throw new IOException("Event journal exceeds capacity");
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length < 36) throw new IOException("Truncated event journal");
            byte[] payload = Arrays.copyOfRange(bytes, 32, bytes.length);
            if (!MessageDigest.isEqual(Arrays.copyOf(bytes, 32), digest(payload))) throw new IOException("Event checksum mismatch");
            try (var input = new DataInputStream(new ByteArrayInputStream(payload))) {
                var image = codec.read(input);
                if (input.available() != 0) throw new IOException("Trailing event journal bytes");
                return image;
            }
        } catch (IOException failure) { throw new UncheckedIOException("Cannot load event journal: " + path, failure); }
    }
    public void commit(EventJournalImage image) {
        Path temporary = path.resolveSibling(path.getFileName() + ".writing");
        try {
            Files.createDirectories(path.getParent());
            var bytes = new ByteArrayOutputStream();
            try (var output = new DataOutputStream(bytes)) { codec.write(output, image); }
            byte[] payload = bytes.toByteArray();
            if (payload.length > 256 * 1024 * 1024 - 32) throw new IOException("Event journal exceeds capacity");
            try (var channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                writeFully(channel, ByteBuffer.wrap(digest(payload)));
                writeFully(channel, ByteBuffer.wrap(payload));
                channel.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) { throw new UncheckedIOException("Cannot commit event journal: " + path, failure); }
    }
    private static void writeFully(FileChannel channel, ByteBuffer bytes) throws IOException {
        while (bytes.hasRemaining()) channel.write(bytes);
    }
    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }
}
