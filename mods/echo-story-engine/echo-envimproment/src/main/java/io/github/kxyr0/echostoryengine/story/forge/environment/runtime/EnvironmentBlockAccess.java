package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.BlockPropertyDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import java.util.Optional;

/**
 * Minimal block-world adapter used by EnvironmentService.
 */
public interface EnvironmentBlockAccess {
    NamespacedId dimensionId();

    BlockStateView air();

    Optional<BlockView> block(NamespacedId blockId);

    Optional<PreparedBlockEntityData> prepareBlockEntityData(
            BlockStateView state,
            Optional<String> nbt) throws EnvironmentApplyException;

    void setBlock(
            EnvironmentPosition position,
            BlockStateView state,
            Optional<PreparedBlockEntityData> blockEntityData) throws EnvironmentApplyException;

    interface BlockStateView {
    }

    interface PreparedBlockEntityData {
    }

    interface BlockView {
        NamespacedId id();

        BlockStateView defaultState();

        BlockStateView withProperty(
                BlockStateView state,
                BlockPropertyDef property) throws EnvironmentApplyException;
    }
}
