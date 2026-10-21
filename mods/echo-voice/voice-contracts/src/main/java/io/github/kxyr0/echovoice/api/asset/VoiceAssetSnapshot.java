package io.github.kxyr0.echovoice.api.asset;

import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetRevision;
import java.util.Objects;

/** Immutable read-only metadata for one committed asset revision. */
public record VoiceAssetSnapshot(
        VoiceAssetKey key,
        VoiceAssetRevision revision,
        VoiceAssetFormat format,
        String sourcePath,
        long byteSize,
        long durationMillis
) {
    public VoiceAssetSnapshot {
        key = Objects.requireNonNull(key, "key");
        revision = Objects.requireNonNull(revision, "revision");
        format = Objects.requireNonNull(format, "format");
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        if (byteSize < 0 || durationMillis < 0) {
            throw new IllegalArgumentException("Asset size and duration must not be negative.");
        }
    }
}
