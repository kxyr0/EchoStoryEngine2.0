package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.BlockPropertyDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class FakeEnvironmentBlockAccess implements EnvironmentBlockAccess {
    private final NamespacedId dimensionId;
    private final Map<NamespacedId, FakeBlock> blocks = new HashMap<>();
    private final Map<EnvironmentPosition, String> placed = new HashMap<>();

    FakeEnvironmentBlockAccess(NamespacedId dimensionId) {
        this.dimensionId = dimensionId;
    }

    void register(String blockId, Map<String, Set<String>> properties) {
        blocks.put(NamespacedId.of(blockId), new FakeBlock(NamespacedId.of(blockId), properties));
    }

    Map<EnvironmentPosition, String> placed() {
        return Map.copyOf(placed);
    }

    @Override
    public NamespacedId dimensionId() {
        return dimensionId;
    }

    @Override
    public BlockStateView air() {
        return new FakeState(NamespacedId.of("minecraft:air"), Map.of());
    }

    @Override
    public Optional<BlockView> block(NamespacedId blockId) {
        return Optional.ofNullable(blocks.get(blockId));
    }

    @Override
    public Optional<PreparedBlockEntityData> prepareBlockEntityData(
            BlockStateView state,
            Optional<String> nbt) {
        return Optional.empty();
    }

    @Override
    public void setBlock(
            EnvironmentPosition position,
            BlockStateView state,
            Optional<PreparedBlockEntityData> blockEntityData) {
        placed.put(position, state.toString());
    }

    private record FakeBlock(
            NamespacedId id,
            Map<String, Set<String>> properties) implements EnvironmentBlockAccess.BlockView {
        @Override
        public EnvironmentBlockAccess.BlockStateView defaultState() {
            return new FakeState(id, Map.of());
        }

        @Override
        public EnvironmentBlockAccess.BlockStateView withProperty(
                EnvironmentBlockAccess.BlockStateView state,
                BlockPropertyDef property) throws EnvironmentApplyException {
            Set<String> accepted = properties.get(property.name());
            if (accepted == null) {
                throw new EnvironmentApplyException("Block '" + id.asString()
                        + "' does not contain property '" + property.name() + "'.");
            }
            if (!accepted.contains(property.value())) {
                throw new EnvironmentApplyException("Property '" + property.name()
                        + "' does not accept value '" + property.value() + "'.");
            }
            return ((FakeState) state).with(property);
        }
    }

    private record FakeState(
            NamespacedId blockId,
            Map<String, String> properties) implements EnvironmentBlockAccess.BlockStateView {
        FakeState with(BlockPropertyDef property) {
            Map<String, String> next = new HashMap<>(properties);
            next.put(property.name(), property.value());
            return new FakeState(blockId, next);
        }

        @Override
        public String toString() {
            if (properties.isEmpty()) {
                return blockId.asString();
            }
            String values = properties.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
            return blockId.asString() + "[" + values + "]";
        }
    }
}
