package io.github.kxyr0.echostoryengine.story.forge.externalcontent;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Copies embedded story seed files into the external pack directory without overwriting authored edits.
 */
public final class ExternalContentSeedExtractor {
    public boolean initialize(Path targetRoot, Path seedRoot) throws IOException {
        Path target = Objects.requireNonNull(targetRoot, "targetRoot").toAbsolutePath().normalize();
        Path seed = Objects.requireNonNull(seedRoot, "seedRoot");
        if (Files.exists(target.resolve(ExternalContentPaths.ARCHIVE_MARKER))) {
            return false;
        }
        if (!Files.isDirectory(seed)) {
            throw new IOException("Embedded story seed not found: " + seed);
        }
        if (Files.exists(target)) {
            if (!Files.isDirectory(target)) {
                throw new IOException("External story content path is not a directory: " + target);
            }
            return copyMissingSeed(seed, target);
        }
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempDirectory(parent, ".jdh-seed-");
        try {
            copySeed(seed, temp);
            moveIntoPlace(temp, target);
            return true;
        } catch (FileAlreadyExistsException exception) {
            deleteRecursively(temp);
            return false;
        } catch (IOException | RuntimeException exception) {
            deleteRecursively(temp);
            throw exception;
        }
    }

    private boolean copyMissingSeed(Path seed, Path targetRoot) throws IOException {
        boolean copied = false;
        try (Stream<Path> stream = Files.walk(seed)) {
            for (Path source : stream.filter(Files::isRegularFile).toList()) {
                Path relative = seed.relativize(source);
                Path target = targetRoot.resolve(relative.toString());
                if (Files.exists(target)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
                copied = true;
            }
        }
        return copied;
    }

    private void copySeed(Path seed, Path temp) throws IOException {
        try (Stream<Path> stream = Files.walk(seed)) {
            for (Path source : stream.filter(Files::isRegularFile).toList()) {
                Path relative = seed.relativize(source);
                Path target = temp.resolve(relative.toString());
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, target);
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
