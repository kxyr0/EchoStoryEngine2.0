package io.github.kxyr0.echoplayersleep.core.scenario;

import io.github.kxyr0.echoplayersleep.api.world.BlockId;
import io.github.kxyr0.echoplayersleep.api.world.BlockPosition;
import java.util.Objects;
import java.util.Optional;

public record PlayerSleepBedRule(
        PlayerSleepBedRuleKind kind,
        Optional<BlockId> blockId,
        Optional<BlockPosition> position) {
    public PlayerSleepBedRule {
        kind = Objects.requireNonNull(kind, "kind");
        blockId = Objects.requireNonNull(blockId, "blockId");
        position = Objects.requireNonNull(position, "position");
    }

    public static PlayerSleepBedRule anySupportedBed() {
        return new PlayerSleepBedRule(PlayerSleepBedRuleKind.ANY_SUPPORTED_BED, Optional.empty(), Optional.empty());
    }

    public static PlayerSleepBedRule block(BlockId blockId) {
        return new PlayerSleepBedRule(PlayerSleepBedRuleKind.BLOCK_ID, Optional.of(blockId), Optional.empty());
    }

    public static PlayerSleepBedRule at(BlockPosition position, BlockId expectedBlock) {
        return new PlayerSleepBedRule(PlayerSleepBedRuleKind.BED_AT,
                Optional.of(expectedBlock), Optional.of(position));
    }
}
