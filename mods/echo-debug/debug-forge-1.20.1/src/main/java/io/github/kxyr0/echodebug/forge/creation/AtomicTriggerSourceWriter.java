package io.github.kxyr0.echodebug.forge.creation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class AtomicTriggerSourceWriter {
    public WriteResult write(Path finalPath, String sourceText) throws IOException {
        if (Files.exists(finalPath)) {
            throw new IOException("Trigger source already exists: " + finalPath);
        }
        Files.createDirectories(finalPath.getParent());
        Path tempPath = Files.createTempFile(finalPath.getParent(), finalPath.getFileName().toString(), ".tmp");
        writeBytes(tempPath, sourceText);
        Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE);
        return new WriteResult(tempPath, finalPath, true);
    }

    public boolean rollback(Path finalPath) {
        try {
            return Files.deleteIfExists(finalPath);
        } catch (IOException exception) {
            return false;
        }
    }

    private static void writeBytes(Path tempPath, String sourceText) throws IOException {
        try (FileChannel channel = FileChannel.open(tempPath, StandardOpenOption.WRITE)) {
            ByteBuffer bytes = ByteBuffer.wrap(sourceText.getBytes(StandardCharsets.UTF_8));
            while (bytes.hasRemaining()) {
                channel.write(bytes);
            }
            channel.force(true);
        }
    }

    public record WriteResult(Path temporaryPath, Path finalPath, boolean atomicMove) {
    }
}
