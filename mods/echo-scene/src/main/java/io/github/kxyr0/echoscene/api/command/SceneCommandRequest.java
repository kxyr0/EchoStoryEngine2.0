package io.github.kxyr0.echoscene.api.command;

import io.github.kxyr0.echoscene.api.definition.SceneCommand;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import java.util.Objects;

public record SceneCommandRequest(
        SceneResourceId resourceId,
        SceneLocalId sceneId,
        SceneExecutionId executionId,
        String rootOwner,
        SceneCommand command
) {
    public SceneCommandRequest {
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        sceneId = Objects.requireNonNull(sceneId, "sceneId");
        executionId = Objects.requireNonNull(executionId, "executionId");
        rootOwner = Objects.requireNonNull(rootOwner, "rootOwner");
        command = Objects.requireNonNull(command, "command");
    }
}
