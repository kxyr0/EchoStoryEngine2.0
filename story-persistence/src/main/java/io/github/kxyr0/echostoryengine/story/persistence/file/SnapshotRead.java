package io.github.kxyr0.echostoryengine.story.persistence.file;

import io.github.kxyr0.echostoryengine.story.core.port.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshot;
import java.util.Optional;

record SnapshotRead(
        SnapshotReadStatus status,
        Optional<ExecutionCheckpoint> checkpoint,
        Optional<ExecutionSnapshot> snapshot,
        String diagnosticCode) {
    static SnapshotRead found(ExecutionCheckpoint checkpoint, ExecutionSnapshot snapshot) {
        return new SnapshotRead(
                SnapshotReadStatus.FOUND,
                Optional.of(checkpoint),
                Optional.of(snapshot),
                "");
    }

    static SnapshotRead missing() {
        return new SnapshotRead(SnapshotReadStatus.MISSING, Optional.empty(), Optional.empty(), "");
    }

    static SnapshotRead corrupt(String diagnosticCode) {
        return new SnapshotRead(SnapshotReadStatus.CORRUPT, Optional.empty(), Optional.empty(), diagnosticCode);
    }
}
