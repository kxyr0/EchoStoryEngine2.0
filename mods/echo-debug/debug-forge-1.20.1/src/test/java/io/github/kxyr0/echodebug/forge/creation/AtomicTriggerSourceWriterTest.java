package io.github.kxyr0.echodebug.forge.creation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicTriggerSourceWriterTest {
    private final AtomicTriggerSourceWriter writer = new AtomicTriggerSourceWriter();

    @TempDir
    Path tempDir;

    @Test
    void writesFinalFileAtomicallyAndCanRollback() throws Exception {
        Path target = tempDir.resolve("story").resolve("triggers").resolve("jdh").resolve("house.trigger.es");

        AtomicTriggerSourceWriter.WriteResult result = writer.write(target, "class House : TriggerResource {}");

        assertTrue(result.atomicMove());
        assertTrue(Files.isRegularFile(target));
        assertTrue(writer.rollback(target));
        assertFalse(Files.exists(target));
    }
}
