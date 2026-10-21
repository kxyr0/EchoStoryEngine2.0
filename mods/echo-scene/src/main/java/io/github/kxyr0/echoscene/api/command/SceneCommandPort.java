package io.github.kxyr0.echoscene.api.command;

import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;

public interface SceneCommandPort {
    SceneCommandStart start(SceneCommandRequest request);

    void cancelOwned(SceneExecutionId owner);
}
