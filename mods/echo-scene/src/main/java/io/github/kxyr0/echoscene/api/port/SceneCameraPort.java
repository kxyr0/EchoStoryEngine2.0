package io.github.kxyr0.echoscene.api.port;

import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;

@FunctionalInterface
public interface SceneCameraPort {
    void restoreOwned(SceneExecutionId owner);
}
