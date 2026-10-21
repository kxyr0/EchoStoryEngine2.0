package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.ClearBlockChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentStateDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.SetBlockChangeDef;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogEntry;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Applies authored environment states after fully validating the requested operations.
 */
public final class EnvironmentService {
    private final EnvironmentCatalogState catalogState;

    public EnvironmentService(EnvironmentCatalogState catalogState) {
        this.catalogState = Objects.requireNonNull(catalogState, "catalogState");
    }

    public void set(
            String environmentId,
            String stateId,
            Function<NamespacedId, Optional<EnvironmentBlockAccess>> worlds) throws EnvironmentApplyException {
        NamespacedId id = parseId(environmentId);
        EnvironmentResourceDef resource = resource(id);
        EnvironmentStateDef state = state(resource, stateId);
        EnvironmentBlockAccess world = worlds.apply(resource.dimension())
                .orElseThrow(() -> new EnvironmentApplyException(
                        "Dimension '" + resource.dimension().asString() + "' is not loaded."));
        if (!world.dimensionId().equals(resource.dimension())) {
            throw new EnvironmentApplyException("Environment dimension mismatch for '" + environmentId + "'.");
        }
        List<EnvironmentOperation> operations = plan(state, world);
        for (EnvironmentOperation operation : operations) {
            world.setBlock(operation.position(), operation.state(), operation.blockEntityData());
        }
    }

    private EnvironmentResourceDef resource(NamespacedId id) throws EnvironmentApplyException {
        return catalogState.current().find(id)
                .map(EnvironmentCatalogEntry::resource)
                .orElseThrow(() -> new EnvironmentApplyException(
                        "Environment '" + id.asString() + "' is not loaded."));
    }

    private EnvironmentStateDef state(
            EnvironmentResourceDef resource,
            String stateId) throws EnvironmentApplyException {
        return resource.findState(stateId)
                .orElseThrow(() -> new EnvironmentApplyException(
                        "Environment '" + resource.id().asString() + "' does not contain state '" + stateId + "'."));
    }

    private List<EnvironmentOperation> plan(
            EnvironmentStateDef state,
            EnvironmentBlockAccess world) throws EnvironmentApplyException {
        List<EnvironmentOperation> operations = new ArrayList<>();
        for (EnvironmentChangeDef change : state.changes()) {
            operations.add(operation(change, world));
        }
        return List.copyOf(operations);
    }

    private EnvironmentOperation operation(
            EnvironmentChangeDef change,
            EnvironmentBlockAccess world) throws EnvironmentApplyException {
        if (change instanceof ClearBlockChangeDef clear) {
            return new EnvironmentOperation(clear.position(), world.air(), Optional.empty());
        }
        SetBlockChangeDef set = (SetBlockChangeDef) change;
        EnvironmentBlockAccess.BlockView block = world.block(set.blockId())
                .orElseThrow(() -> new EnvironmentApplyException(
                        "Block '" + set.blockId().asString() + "' is not registered."));
        EnvironmentBlockAccess.BlockStateView state = state(block, set);
        Optional<EnvironmentBlockAccess.PreparedBlockEntityData> data =
                world.prepareBlockEntityData(state, set.blockEntityNbt());
        return new EnvironmentOperation(set.position(), state, data);
    }

    private EnvironmentBlockAccess.BlockStateView state(
            EnvironmentBlockAccess.BlockView block,
            SetBlockChangeDef set) throws EnvironmentApplyException {
        EnvironmentBlockAccess.BlockStateView state = block.defaultState();
        for (var property : set.properties()) {
            state = block.withProperty(state, property);
        }
        return state;
    }

    private NamespacedId parseId(String environmentId) throws EnvironmentApplyException {
        try {
            return NamespacedId.of(environmentId);
        } catch (IllegalArgumentException exception) {
            throw new EnvironmentApplyException("Environment id '" + environmentId + "' is invalid.");
        }
    }
}
