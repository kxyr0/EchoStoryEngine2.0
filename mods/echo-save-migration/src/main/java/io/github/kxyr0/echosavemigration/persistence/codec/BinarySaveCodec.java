package io.github.kxyr0.echosavemigration.persistence.codec;

import io.github.kxyr0.echosavemigration.data.SaveSnapshot;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class BinarySaveCodec implements SaveCodec {
    private final SaveBinaryReader reader = new SaveBinaryReader();
    private final SaveBinaryWriter writer = new SaveBinaryWriter();

    @Override
    public SaveSnapshot read(Path path) throws IOException {
        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            return reader.read(input);
        }
    }

    @Override
    public void write(Path path, SaveSnapshot snapshot) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (var output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)))) {
            writer.write(output, snapshot);
        }
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }
}
