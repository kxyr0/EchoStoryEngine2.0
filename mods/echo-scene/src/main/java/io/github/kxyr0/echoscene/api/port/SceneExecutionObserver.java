package io.github.kxyr0.echoscene.api.port;

import io.github.kxyr0.echoscene.api.model.SceneInspection;

@FunctionalInterface
public interface SceneExecutionObserver {
    void changed(SceneInspection inspection);

    static SceneExecutionObserver quiet() {
        return ignored -> { };
    }
}
