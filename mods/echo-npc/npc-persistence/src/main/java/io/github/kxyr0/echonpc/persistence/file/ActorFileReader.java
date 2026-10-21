package io.github.kxyr0.echonpc.persistence.file;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.core.actor.ActorDesiredState;
import io.github.kxyr0.echonpc.core.persistence.ActorRepositoryResult;
import io.github.kxyr0.echonpc.persistence.schema.ActorSaveCodec;
import io.github.kxyr0.echonpc.persistence.schema.ActorSaveMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;

/**
 * Bounded primary/backup reader for actor save files.
 */
final class ActorFileReader {
    private final ActorFileLayout layout;
    private final ActorSaveMapper mapper;
    private final ActorSaveCodec codec;

    ActorFileReader(ActorFileLayout layout, ActorSaveMapper mapper, ActorSaveCodec codec) {
        this.layout = java.util.Objects.requireNonNull(layout, "layout");
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
        this.codec = java.util.Objects.requireNonNull(codec, "codec");
    }

    ActorRepositoryResult.Load load(ActorId actorId) {
        return readKnown(actorId, layout.primary(actorId), layout.backup(actorId));
    }

    ActorRepositoryResult.LoadAll loadAll(int limit) {
        if (!Files.isDirectory(layout.root())) {
            return new ActorRepositoryResult.LoadAll(java.util.List.of(), false, java.util.List.of());
        }
        return readAllPrimaries(limit);
    }

    int count(String glob) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(layout.root(), glob)) {
            int count = 0;
            for (Path ignored : stream) {
                count++;
            }
            return count;
        } catch (IOException exception) {
            return 0;
        }
    }

    private ActorRepositoryResult.LoadAll readAllPrimaries(int limit) {
        java.util.List<ActorRepositoryResult.Load> records = new java.util.ArrayList<>();
        java.util.List<String> warnings = new java.util.ArrayList<>();
        java.util.Set<ActorId> seen = new HashSet<>();
        boolean truncated = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(layout.root(), "*.json")) {
            for (Path path : stream) {
                if (records.size() >= limit) {
                    truncated = true;
                    break;
                }
                records.add(readEnumerated(path, seen, warnings));
            }
            return new ActorRepositoryResult.LoadAll(records, truncated, warnings);
        } catch (IOException exception) {
            var failed = new ActorRepositoryResult.Load(ActorRepositoryResult.LoadStatus.FAILED,
                    Optional.empty(), exception.getClass().getSimpleName());
            return new ActorRepositoryResult.LoadAll(java.util.List.of(failed), false, warnings);
        }
    }

    private ActorRepositoryResult.Load readEnumerated(Path primary, java.util.Set<ActorId> seen,
            java.util.List<String> warnings) {
        ActorRepositoryResult.Load loaded = readKnown(null, primary, backupFor(primary));
        if (loaded.actor().isEmpty()) {
            return loaded;
        }
        ActorId actorId = loaded.actor().orElseThrow().actorId();
        if (!layout.matches(primary, actorId)) {
            return corrupt("Filename hash mismatch: " + primary.getFileName());
        }
        if (!seen.add(actorId)) {
            return corrupt("Duplicate ActorId record: " + actorId.value());
        }
        if (loaded.message().contains("backup")) {
            warnings.add(loaded.message());
        }
        return loaded;
    }

    private ActorRepositoryResult.Load readKnown(ActorId expected, Path primary, Path backup) {
        if (!Files.isRegularFile(primary)) {
            return new ActorRepositoryResult.Load(ActorRepositoryResult.LoadStatus.NOT_FOUND,
                    Optional.empty(), "Actor record отсутствует.");
        }
        ActorRepositoryResult.Load primaryResult = readFile(primary);
        if (primaryResult.status() == ActorRepositoryResult.LoadStatus.FOUND || isUnsupported(primaryResult)) {
            return primaryResult;
        }
        ActorRepositoryResult.Load backupResult = readFile(backup);
        if (backupResult.status() == ActorRepositoryResult.LoadStatus.FOUND && expectedMatches(expected,
                backupResult.actor().orElseThrow())) {
            return new ActorRepositoryResult.Load(ActorRepositoryResult.LoadStatus.FOUND, backupResult.actor(),
                    "Primary corrupt; loaded backup.");
        }
        return primaryResult.status() == ActorRepositoryResult.LoadStatus.NOT_FOUND ? primaryResult
                : corrupt("Primary and backup are corrupt or unavailable.");
    }

    private ActorRepositoryResult.Load readFile(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return new ActorRepositoryResult.Load(ActorRepositoryResult.LoadStatus.NOT_FOUND,
                        Optional.empty(), "Actor record отсутствует.");
            }
            ActorDesiredState state = mapper.toState(codec.decode(Files.readString(file, StandardCharsets.UTF_8)));
            return new ActorRepositoryResult.Load(ActorRepositoryResult.LoadStatus.FOUND, Optional.of(state),
                    "Actor record loaded.");
        } catch (UnsupportedOperationException exception) {
            return new ActorRepositoryResult.Load(ActorRepositoryResult.LoadStatus.UNSUPPORTED_SCHEMA,
                    Optional.empty(), exception.getMessage());
        } catch (RuntimeException exception) {
            return corrupt(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        } catch (IOException exception) {
            return new ActorRepositoryResult.Load(ActorRepositoryResult.LoadStatus.FAILED, Optional.empty(),
                    exception.getClass().getSimpleName());
        }
    }

    private Path backupFor(Path primary) {
        return primary.resolveSibling(primary.getFileName().toString() + ".bak");
    }

    private boolean expectedMatches(ActorId expected, ActorDesiredState state) {
        return expected == null || expected.equals(state.actorId());
    }

    private boolean isUnsupported(ActorRepositoryResult.Load result) {
        return result.status() == ActorRepositoryResult.LoadStatus.UNSUPPORTED_SCHEMA;
    }

    private ActorRepositoryResult.Load corrupt(String message) {
        return new ActorRepositoryResult.Load(ActorRepositoryResult.LoadStatus.CORRUPT, Optional.empty(), message);
    }
}
