package io.github.kxyr0.echoscene.api.port;

import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.runtime.SceneCheckpoint;
import java.util.Optional;

public interface SceneCheckpointStore {
    void save(SceneCheckpoint checkpoint);

    Optional<SceneCheckpoint> load(SceneExecutionId executionId);
}
