package io.github.kxyr0.echoteleportmagic.forge.persistence;

import io.github.kxyr0.echoteleportmagic.api.TeleportExecution;
import io.github.kxyr0.echoteleportmagic.api.TeleportExecutionId;
import io.github.kxyr0.echoteleportmagic.runtime.TeleportPort.ReceiptStage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Forced write-ahead receipt; an ambiguous intent is never automatically replayed. */
public final class TeleportReceiptStore {
    private final Path directory;
    public TeleportReceiptStore(Path directory) { this.directory = directory; }

    public ReceiptStage read(TeleportExecutionId id) {
        Path file = path(id);
        if (!Files.exists(file)) return ReceiptStage.NONE;
        try (var input = new DataInputStream(Files.newInputStream(file))) {
            if (input.readInt() != 1) throw new IOException("Receipt schema");
            return ReceiptStage.valueOf(input.readUTF());
        } catch (IOException | IllegalArgumentException error) { throw new IllegalStateException("Receipt read failed", error); }
    }
    public void validate(TeleportExecution execution) {
        if (!Files.exists(path(execution.id()))) return;
        try (var in = new DataInputStream(Files.newInputStream(path(execution.id())))) {
            in.readInt(); in.readUTF();
            if (!in.readUTF().equals(fingerprint(execution))) throw new IOException("Execution ID reused with changed request");
        } catch (IOException error) { throw new IllegalStateException("Receipt request mismatch", error); }
    }
    public void write(TeleportExecution execution, ReceiptStage stage) {
        try {
            Files.createDirectories(directory);
            Path temporary = directory.resolve(execution.id().value() + ".pending");
            try (var out = new DataOutputStream(Files.newOutputStream(temporary))) {
                out.writeInt(1); out.writeUTF(stage.name()); out.writeUTF(fingerprint(execution));
                out.writeBoolean(execution.committed());
            }
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temporary, path(execution.id()), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) { throw new IllegalStateException("Receipt persistence failed", error); }
    }
    private String fingerprint(TeleportExecution e) {
        var d = e.destination();
        return e.subjectReference() + "|" + d.locationId() + "|" + d.dimension() + "|" + d.x() + "|" + d.y() + "|" + d.z()
                + "|" + e.preview() + "|" + e.profileRevision();
    }
    private Path path(TeleportExecutionId id) { return directory.resolve(id.value() + ".receipt"); }
}
