package io.github.kxyr0.echostoryengine.story.persistence.schema;

import java.util.Objects;

/**
 * Strict JSON codec for save schema V1 snapshots.
 */
public final class ExecutionSnapshotCodec {
    private final SnapshotJsonWriter writer = new SnapshotJsonWriter();
    private final SnapshotJsonReader reader = new SnapshotJsonReader();

    /**
     * Encodes a snapshot with deterministic field and map ordering.
     */
    public String encode(ExecutionSnapshot snapshot) {
        return writer.write(Objects.requireNonNull(snapshot, "snapshot"));
    }

    /**
     * Decodes and validates one schema V1 snapshot.
     */
    public ExecutionSnapshot decode(String content) throws SnapshotDecodeException {
        return reader.read(Objects.requireNonNull(content, "content"));
    }
}
