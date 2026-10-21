package io.github.kxyr0.echocore.core;

import io.github.kxyr0.echocore.api.ChapterResult.Code;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public final class ChapterFiles {
    private ChapterFiles() { }

    public static void ordinary(Path path) throws IOException {
        if (Files.isSymbolicLink(path) || !path.toRealPath().equals(path.toAbsolutePath().normalize())) {
            throw new IOException("Symlinks and junctions are not allowed: " + path);
        }
    }

    public static Lease lock(Path world, Code error) throws IOException {
        Path file = world.resolve("session.lock");
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) ordinary(file);
        FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) throw new ChapterFailure(error, "World is locked: " + world);
            return new Lease(channel, lock);
        } catch (IOException | OverlappingFileLockException failure) {
            channel.close();
            if (failure instanceof ChapterFailure typed) throw typed;
            throw new ChapterFailure(error, "Cannot acquire world lock: " + world);
        }
    }

    public static void copy(Path source, Path target) throws IOException {
        ordinary(source);
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                ordinary(dir);
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                ordinary(file);
                if (!attrs.isRegularFile()) throw new IOException("Not an ordinary file: " + file);
                Path relative = source.relativize(file);
                if (!relative.toString().equals("session.lock") && !relative.toString().equals(".echo-core-owner")) {
                    Files.copy(file, target.resolve(relative));
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteOwned(Path root, Path directory, String chapter) throws IOException {
        Path normalized = directory.toAbsolutePath().normalize();
        if (!normalized.getParent().equals(root) || !normalized.startsWith(root)) throw new IOException("Unsafe delete target.");
        ordinary(normalized);
        Path marker = normalized.resolve(".echo-core-owner");
        ordinary(marker);
        if (!Files.readString(marker).equals(chapter)) throw new IOException("Unowned delete target.");
        Files.walkFileTree(normalized, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                ordinary(dir);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
                if (failure != null) throw failure;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public record Lease(FileChannel channel, FileLock lock) implements AutoCloseable {
        @Override public void close() throws IOException { lock.release(); channel.close(); }
    }
}
