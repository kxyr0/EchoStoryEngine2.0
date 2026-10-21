package io.github.kxyr0.echocore.core;

import io.github.kxyr0.echocore.api.ChapterDefinition;
import io.github.kxyr0.echocore.api.ChapterResult.Code;
import io.github.kxyr0.echocore.api.ChapterSnapshot.State;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterSaveStoreTest {
    @TempDir Path directory;
    private Path template() throws IOException {
        Path template = directory.resolve("world");
        Files.createDirectories(template.resolve("region"));
        Files.writeString(template.resolve("level.dat"), "template level data");
        Files.writeString(template.resolve("region/r.0.0.mca"), "original blocks");
        Files.writeString(template.resolve("session.lock"), "lock data");
        return template;
    }
    private ChapterSaveStore store() throws IOException {
        return new ChapterSaveStore(directory.resolve("saves"), world -> {
            Path data = world.resolve("level.dat");
            if (!Files.isRegularFile(data) || Files.size(data) == 0) throw new IOException("Invalid world data");
        });
    }

    @Test void newSaveExitContinueRestartCycleDoesNotTouchTemplate() throws Exception {
        var chapter = new ChapterDefinition("chapter_1", template());
        var store = store();
        assertEquals(State.NO_SAVE, store.state(chapter));
        assertEquals(Code.NO_SAVE, assertThrows(ChapterFailure.class, () -> store.continueChapter(chapter)).code());
        store.create(chapter, false);
        assertEquals(State.READY, store.state(chapter));
        Files.writeString(store.save(chapter).resolve("region/r.0.0.mca"), "player changes");
        Files.writeString(store.save(chapter).resolve("progress.tmp"), "old run");
        // A fresh backend reads the existing disk save and keeps the same playthrough.
        var reopened = store();
        reopened.continueChapter(chapter);
        reopened.create(chapter, false);
        assertEquals("player changes", Files.readString(reopened.save(chapter).resolve("region/r.0.0.mca")));
        reopened.create(chapter, true);
        assertEquals("original blocks", Files.readString(reopened.save(chapter).resolve("region/r.0.0.mca")));
        assertFalse(Files.exists(reopened.save(chapter).resolve("progress.tmp")));
        assertEquals("original blocks", Files.readString(chapter.template().resolve("region/r.0.0.mca")));
        assertEquals("lock data", Files.readString(chapter.template().resolve("session.lock")));
    }

    @Test void restartWithoutPriorSaveCreatesFirstRunAndChaptersAreIsolated() throws Exception {
        Path template = template();
        var first = new ChapterDefinition("chapter_1", template);
        var second = new ChapterDefinition("chapter_2", template);
        var store = store();
        store.create(first, true); store.create(second, false);
        Files.writeString(store.save(second).resolve("keep.txt"), "chapter 2 data");
        store.create(first, true);
        assertEquals("chapter 2 data", Files.readString(store.save(second).resolve("keep.txt")));
    }

    @Test void missingAndInvalidTemplatesPreserveOldRun() throws Exception {
        var chapter = new ChapterDefinition("chapter_1", template());
        var store = store(); store.create(chapter, false);
        Files.writeString(store.save(chapter).resolve("keep.txt"), "old run");
        Files.writeString(chapter.template().resolve("level.dat"), "");
        assertEquals(Code.TEMPLATE_INVALID, assertThrows(ChapterFailure.class, () -> store.create(chapter, true)).code());
        assertEquals("old run", Files.readString(store.save(chapter).resolve("keep.txt")));
        var missing = new ChapterDefinition("chapter_1", directory.resolve("missing"));
        assertEquals(Code.TEMPLATE_MISSING, assertThrows(ChapterFailure.class, () -> store.create(missing, true)).code());
        assertTrue(Files.exists(store.save(chapter).resolve("keep.txt")));
    }

    @Test void rejectsForeignSaveBeforeAnyDeletion() throws Exception {
        var chapter = new ChapterDefinition("chapter_1", template());
        var store = store(); Files.createDirectories(store.save(chapter));
        Files.writeString(store.save(chapter).resolve("keep.txt"), "foreign world");
        assertEquals(Code.FOREIGN_SAVE, assertThrows(ChapterFailure.class, () -> store.create(chapter, true)).code());
        assertEquals("foreign world", Files.readString(store.save(chapter).resolve("keep.txt")));
    }

    @Test void lockedTemplateAndLockedSaveAreRejected() throws Exception {
        var chapter = new ChapterDefinition("chapter_1", template());
        var store = store();
        try (var channel = FileChannel.open(chapter.template().resolve("session.lock"), StandardOpenOption.WRITE);
                var lock = channel.lock()) {
            assertEquals(Code.TEMPLATE_LOCKED, assertThrows(ChapterFailure.class, () -> store.create(chapter, false)).code());
        }
        store.create(chapter, false); store.continueChapter(chapter);
        try (var channel = FileChannel.open(store.save(chapter).resolve("session.lock"), StandardOpenOption.WRITE);
                var lock = channel.lock()) {
            assertEquals(Code.SAVE_LOCKED, assertThrows(ChapterFailure.class, () -> store.continueChapter(chapter)).code());
            assertEquals(Code.SAVE_LOCKED, assertThrows(ChapterFailure.class, () -> store.create(chapter, true)).code());
        }
    }

    @Test void corruptSaveIsReportedAndCanBeRestarted() throws Exception {
        var chapter = new ChapterDefinition("chapter_1", template());
        var store = store(); store.create(chapter, false);
        Files.writeString(store.save(chapter).resolve("level.dat"), "");
        assertEquals(State.INVALID_SAVE, store.state(chapter));
        assertEquals(Code.INVALID_SAVE, assertThrows(ChapterFailure.class, () -> store.continueChapter(chapter)).code());
        store.create(chapter, true);
        assertEquals(State.READY, store.state(chapter));
    }

    @Test void rejectsPathTraversalAndOverlappingTemplate() throws Exception {
        for (String id : new String[] {"../world", "C:/world", "chapter/1", "", "chapter_1/../../world"}) {
            assertThrows(IllegalArgumentException.class, () -> new ChapterDefinition(id, directory));
        }
        var store = store();
        var chapter = new ChapterDefinition("chapter_1", directory);
        assertEquals(Code.TEMPLATE_INVALID, assertThrows(ChapterFailure.class, () -> store.create(chapter, true)).code());
    }

    @Test void failedStagingValidationPreservesOldSaveAndCleansPreparedCopy() throws Exception {
        var chapter = new ChapterDefinition("chapter_1", template());
        var original = store(); original.create(chapter, false);
        Files.writeString(original.save(chapter).resolve("keep.txt"), "old progress");
        var failing = new ChapterSaveStore(directory.resolve("saves"), world -> {
            if (world.getFileName().toString().endsWith(".prepare")) throw new IOException("Simulated incomplete template copy");
        });
        assertThrows(IOException.class, () -> failing.create(chapter, true));
        assertEquals("old progress", Files.readString(original.save(chapter).resolve("keep.txt")));
        try (var children = Files.list(directory.resolve("saves"))) { assertEquals(1, children.count()); }
    }
}
