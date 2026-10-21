package io.github.kxyr0.echosavemigration.api;

import java.time.Instant;
import java.util.Objects;

public record MigrationReceipt(
        MigrationId migrationId,
        SaveSchemaVersion from,
        SaveSchemaVersion to,
        String contentFingerprint,
        long revision,
        Instant appliedAt
) {
    public MigrationReceipt {
        migrationId = Objects.requireNonNull(migrationId, "migrationId");
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
        contentFingerprint = Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        appliedAt = Objects.requireNonNull(appliedAt, "appliedAt");
    }
}
