package io.github.kxyr0.echosavemigration.api;

import java.util.Map;
import java.util.Objects;

public record EchoSaveManifest(
        SaveSchemaVersion schema,
        long migrationRevision,
        String contentFingerprint,
        Map<String, Integer> moduleVersions
) {
    public EchoSaveManifest {
        schema = Objects.requireNonNull(schema, "schema");
        contentFingerprint = Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        moduleVersions = Map.copyOf(moduleVersions);
        if (migrationRevision < 0 || moduleVersions.values().stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("Persistence revisions must not be negative.");
        }
    }

    public EchoSaveManifest migrated(SaveSchemaVersion target, long revision) {
        return new EchoSaveManifest(target, revision, contentFingerprint, moduleVersions);
    }
}
