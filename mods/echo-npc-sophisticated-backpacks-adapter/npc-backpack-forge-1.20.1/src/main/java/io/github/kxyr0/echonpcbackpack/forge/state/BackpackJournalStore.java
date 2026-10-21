package io.github.kxyr0.echonpcbackpack.forge.state;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/**
 * Synchronous atomic journal used before and after every destructive boundary.
 */
public final class BackpackJournalStore {
    private static final int SCHEMA_VERSION = 1;
    private final BackpackNbtCodec codec = new BackpackNbtCodec();
    private final Path path;

    public BackpackJournalStore(Path worldRoot) {
        path = worldRoot.resolve("data").resolve("echo_npc_sophisticated_backpacks.dat");
    }

    public synchronized BackpackJournalSnapshot load() throws IOException {
        if (!Files.isRegularFile(path)) return BackpackJournalSnapshot.empty();
        CompoundTag root;
        try (InputStream input = Files.newInputStream(path)) {
            root = NbtIo.readCompressed(input);
        }
        if (root.getInt("SchemaVersion") != SCHEMA_VERSION) {
            throw new IOException("Unsupported backpack journal schema: " + root.getInt("SchemaVersion"));
        }
        Map<String, BackpackActorRecord> actors = new LinkedHashMap<>();
        for (Tag item : root.getList("Actors", Tag.TAG_COMPOUND)) {
            BackpackActorRecord record = codec.read((CompoundTag) item);
            actors.put(record.actorId.value(), record);
        }
        Map<String, String> commands = new LinkedHashMap<>();
        for (Tag item : root.getList("TerminalCommands", Tag.TAG_COMPOUND)) {
            CompoundTag command = (CompoundTag) item;
            commands.put(command.getString("Id"), command.getString("Outcome"));
        }
        return new BackpackJournalSnapshot(actors, commands, root.getLong("RepositoryRevision"));
    }

    public synchronized void save(BackpackJournalSnapshot snapshot) throws IOException {
        Files.createDirectories(path.getParent());
        CompoundTag root = new CompoundTag();
        root.putInt("SchemaVersion", SCHEMA_VERSION);
        root.putLong("RepositoryRevision", snapshot.repositoryRevision());
        ListTag actors = new ListTag();
        snapshot.actors().values().forEach(record -> actors.add(codec.write(record)));
        root.put("Actors", actors);
        ListTag commands = new ListTag();
        snapshot.terminalCommands().forEach((id, outcome) -> {
            CompoundTag command = new CompoundTag();
            command.putString("Id", id);
            command.putString("Outcome", outcome);
            commands.add(command);
        });
        root.put("TerminalCommands", commands);
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            NbtIo.writeCompressed(root, output);
        }
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
        moveAtomically(temporary, path);
    }

    public Path path() {
        return path;
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
