package io.github.kxyr0.echoobservation.forge.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.core.progress.ObservationProgressBook;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ObservationDurableStoreTest {
    @TempDir Path directory;
    @Test void restartKeepsProgressAndLargePreparedOutboxInSameSnapshot() {
        var data = new ObservationProgressSavedData(new ObservationProgressBook());
        var player = UUID.randomUUID();
        var resource = ObservationResourceId.parse("jdh:room");
        data.book().player(player).activate(resource);
        data.outbox("completion-1", "x".repeat(100_000));
        var tag = data.save(new CompoundTag());
        tag.putInt("ee1Schema", 1);
        var file = directory.resolve("progress.nbt");
        ObservationDurableStore.write(file, tag);
        var restored = ObservationDurableStore.load(file).orElseThrow();
        assertTrue(restored.book().player(player).activeResources().contains(resource));
        assertEquals(data.outboxes(), restored.outboxes());
    }
    @Test void corruptSnapshotFailsClosedAndFailedMutationRestoresMemory() throws Exception {
        var file = directory.resolve("progress.nbt");
        Files.writeString(file, "interrupted data");
        assertThrows(IllegalStateException.class, () -> ObservationDurableStore.load(file));
        var data = new ObservationProgressSavedData(new ObservationProgressBook());
        assertThrows(IllegalStateException.class, () -> ObservationDurableStore.mutate(null, data, () -> {
            data.outbox("uncommitted", "data");
            throw new IllegalStateException("simulated source commit failure");
        }));
        assertTrue(data.outboxes().isEmpty());
    }
}
