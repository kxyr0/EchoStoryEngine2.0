package io.github.kxyr0.echocore.core;

import io.github.kxyr0.echocore.api.ChapterDefinition;
import io.github.kxyr0.echocore.api.ChapterResult.Code;
import io.github.kxyr0.echocore.api.ChapterSnapshot.State;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class ChapterSaveStore {
    @FunctionalInterface public interface WorldValidator { void validate(Path world) throws IOException; }
    private final Path root;
    private final WorldValidator validator;

    public ChapterSaveStore(Path saves, WorldValidator validator) throws IOException {
        Files.createDirectories(saves);
        root = saves.toRealPath();
        this.validator = validator;
    }
    public Path save(ChapterDefinition chapter) { return root.resolve(chapter.saveId()); }
    public State state(ChapterDefinition chapter) {
        Path save = save(chapter);
        if (!Files.exists(save, LinkOption.NOFOLLOW_LINKS)) return State.NO_SAVE;
        try { owned(save, chapter.id()); validator.validate(save); return State.READY; }
        catch (IOException failure) { return State.INVALID_SAVE; }
    }
    public boolean exists(ChapterDefinition chapter) { return Files.exists(save(chapter), LinkOption.NOFOLLOW_LINKS); }
    public boolean templateExists(ChapterDefinition chapter) { return Files.isRegularFile(chapter.template().resolve("level.dat")); }

    public void continueChapter(ChapterDefinition chapter) throws IOException {
        Path target = save(chapter);
        if (!exists(chapter)) throw new ChapterFailure(Code.NO_SAVE, "No save exists for " + chapter.id());
        owned(target, chapter.id());
        try (var lease = ChapterFiles.lock(target, Code.SAVE_LOCKED)) {
            try { validator.validate(target); }
            catch (IOException failure) { throw new ChapterFailure(Code.INVALID_SAVE, failure.getMessage()); }
        }
    }

    public synchronized void create(ChapterDefinition chapter, boolean restart) throws IOException {
        Path target = save(chapter), template = chapter.template();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            owned(target, chapter.id());
            if (!restart) return;
            try (var lease = ChapterFiles.lock(target, Code.SAVE_LOCKED)) { /* verify exclusive ownership */ }
        }
        if (!Files.isDirectory(template)) throw new ChapterFailure(Code.TEMPLATE_MISSING, "Template missing: " + template);
        ChapterFiles.ordinary(template);
        if (template.startsWith(target) || target.startsWith(template)) throw new ChapterFailure(Code.TEMPLATE_INVALID, "Template overlaps save.");
        String transaction = UUID.randomUUID().toString();
        Path stage = root.resolve(".echo-core-" + chapter.id() + "-" + transaction + ".prepare");
        Path backup = root.resolve(".echo-core-" + chapter.id() + "-" + transaction + ".previous");
        boolean promoted = false;
        Files.createDirectory(stage);
        Files.writeString(stage.resolve(".echo-core-owner"), chapter.id());
        try {
            try (var lease = ChapterFiles.lock(template, Code.TEMPLATE_LOCKED)) {
                try { validator.validate(template); }
                catch (IOException failure) { throw new ChapterFailure(Code.TEMPLATE_INVALID, failure.getMessage()); }
                ChapterFiles.copy(template, stage);
                validator.validate(stage);
            }
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                owned(target, chapter.id());
                // Windows cannot rename a directory while its own lock handle is open.
                try (var lease = ChapterFiles.lock(target, Code.SAVE_LOCKED)) { /* recheck immediately before commit */ }
                move(target, backup);
                promote(stage, target, backup);
            } else {
                promote(stage, target, backup);
            }
            promoted = true;
        } finally {
            if (!promoted && Files.exists(stage)) ChapterFiles.deleteOwned(root, stage, chapter.id());
        }
        if (Files.exists(backup)) {
            try { ChapterFiles.deleteOwned(root, backup, chapter.id()); }
            catch (IOException failure) { System.getLogger("echo-core").log(System.Logger.Level.WARNING, "Old save cleanup deferred: " + backup, failure); }
        }
    }

    private static void promote(Path stage, Path target, Path backup) throws IOException {
        try { move(stage, target); }
        catch (IOException failure) {
            if (Files.exists(backup) && !Files.exists(target)) {
                try { move(backup, target); }
                catch (IOException rollback) { failure.addSuppressed(rollback); }
            }
            throw failure;
        }
    }

    private static void owned(Path target, String id) throws IOException {
        ChapterFiles.ordinary(target);
        Path marker = target.resolve(".echo-core-owner");
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)) throw new ChapterFailure(Code.FOREIGN_SAVE, "Save is not owned by Echo Core: " + target);
        ChapterFiles.ordinary(marker);
        if (!Files.readString(marker).equals(id)) throw new ChapterFailure(Code.FOREIGN_SAVE, "Save owner does not match " + id);
    }
    private static void move(Path from, Path to) throws IOException {
        try { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException failure) { Files.move(from, to); }
    }
}
