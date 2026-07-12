package io.github.kxyr0.echostoryengine.story.persistence.file;

import io.github.kxyr0.echostoryengine.story.core.port.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshot;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshotCodec;
import io.github.kxyr0.echostoryengine.story.persistence.schema.ExecutionSnapshotTranslator;
import io.github.kxyr0.echostoryengine.story.persistence.schema.SnapshotDecodeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class SnapshotFileReader {
    private final ExecutionSnapshotCodec codec;
    private final ExecutionSnapshotTranslator translator;

    SnapshotFileReader(ExecutionSnapshotCodec codec, ExecutionSnapshotTranslator translator) {
        this.codec = codec;
        this.translator = translator;
    }

    SnapshotRead read(Path path) {
        if (!Files.exists(path)) {
            return SnapshotRead.missing();
        }
        try {
            ExecutionSnapshot snapshot = codec.decode(Files.readString(path, StandardCharsets.UTF_8));
            ExecutionCheckpoint checkpoint = translator.toCheckpoint(snapshot);
            return SnapshotRead.found(checkpoint, snapshot);
        } catch (SnapshotDecodeException exception) {
            return SnapshotRead.corrupt(exception.diagnosticCode());
        } catch (IOException | RuntimeException exception) {
            return SnapshotRead.corrupt("ESE-PERSISTENCE-004");
        }
    }
}
