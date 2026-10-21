package io.github.kxyr0.echoobservation.forge.persistence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Atomic authoritative progress snapshot; vanilla SavedData remains a compatible mirror. */
public final class ObservationDurableStore {
    private ObservationDurableStore() { }
    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("echo/observations/progress.nbt");
    }
    static Optional<ObservationProgressSavedData> load(MinecraftServer server) {
        return load(path(server));
    }
    static Optional<ObservationProgressSavedData> load(Path file) {
        if (!Files.exists(file)) return Optional.empty();
        try (var input = Files.newInputStream(file)) {
            var tag = NbtIo.readCompressed(input);
            if (tag.getInt("ee1Schema") != 1 || !tag.contains("players", net.minecraft.nbt.Tag.TAG_LIST)) throw new IOException("Unsupported Observation progress schema");
            return Optional.of(ObservationProgressSavedData.fromTag(tag));
        } catch (IOException failure) { throw new IllegalStateException("Cannot restore Observation progress", failure); }
    }
    public static void commit(MinecraftServer server, ObservationProgressSavedData data) {
        if (server == null) return;
        CompoundTag tag = data.save(new CompoundTag());
        tag.putInt("ee1Schema", 1);
        write(path(server), tag);
    }
    static void write(Path file, CompoundTag tag) {
        Path temporary = file.resolveSibling(file.getFileName() + ".writing");
        try {
            Files.createDirectories(file.getParent());
            var bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, bytes);
            try (var channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                var buffer = ByteBuffer.wrap(bytes.toByteArray());
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) { throw new IllegalStateException("Cannot commit Observation progress", failure); }
    }
    public static void mutate(MinecraftServer server, ObservationProgressSavedData data, Runnable change) {
        var before = data.save(new CompoundTag());
        try {
            change.run();
            data.setDirty();
            commit(server, data);
        } catch (RuntimeException failure) {
            data.restore(before);
            throw failure;
        }
    }
}
