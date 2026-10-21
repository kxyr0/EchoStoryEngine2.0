package io.github.kxyr0.echosavemigration.persistence.codec;

import io.github.kxyr0.echosavemigration.data.SaveSnapshot;
import java.io.IOException;
import java.nio.file.Path;

public interface SaveCodec {
    SaveSnapshot read(Path path) throws IOException;

    void write(Path path, SaveSnapshot snapshot) throws IOException;
}
