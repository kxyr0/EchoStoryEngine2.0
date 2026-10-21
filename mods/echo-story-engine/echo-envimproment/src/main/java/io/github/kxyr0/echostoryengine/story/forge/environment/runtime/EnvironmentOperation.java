package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import java.util.Objects;
import java.util.Optional;

record EnvironmentOperation(
        EnvironmentPosition position,
        EnvironmentBlockAccess.BlockStateView state,
        Optional<EnvironmentBlockAccess.PreparedBlockEntityData> blockEntityData) {
    EnvironmentOperation {
        position = Objects.requireNonNull(position, "position");
        state = Objects.requireNonNull(state, "state");
        blockEntityData = Objects.requireNonNull(blockEntityData, "blockEntityData");
    }
}
