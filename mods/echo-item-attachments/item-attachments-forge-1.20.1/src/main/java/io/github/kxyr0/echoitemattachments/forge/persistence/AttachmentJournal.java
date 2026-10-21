package io.github.kxyr0.echoitemattachments.forge.persistence;

import io.github.kxyr0.echoitemattachments.core.model.CustodyRecord;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

public final class AttachmentJournal {
    private static final int SCHEMA = 1;
    private final Path path;
    private final AttachmentRecordNbt codec = new AttachmentRecordNbt();

    public AttachmentJournal(Path worldRoot) {
        path = worldRoot.resolve("data").resolve("echo_item_attachments.dat");
    }

    public synchronized List<CustodyRecord> load() {
        if (!Files.exists(path)) return List.of();
        try {
            CompoundTag root = NbtIo.readCompressed(path.toFile());
            if (root.getInt("schema") != SCHEMA) {
                throw new IllegalStateException("Unsupported item attachment journal schema");
            }
            List<CustodyRecord> records = new ArrayList<>();
            ListTag list = root.getList("records", Tag.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) records.add(codec.read(list.getCompound(index)));
            return List.copyOf(records);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read item attachment journal", exception);
        }
    }

    public synchronized void commit(Collection<CustodyRecord> records) {
        try {
            Files.createDirectories(path.getParent());
            CompoundTag root = new CompoundTag();
            root.putInt("schema", SCHEMA);
            ListTag list = new ListTag();
            records.stream().sorted((a, b) -> a.attachmentId().compareTo(b.attachmentId()))
                    .map(codec::write).forEach(list::add);
            root.put("records", list);
            writeAtomically(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not commit item attachment journal", exception);
        }
    }

    private void writeAtomically(CompoundTag root) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, bytes);
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes.toByteArray());
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
