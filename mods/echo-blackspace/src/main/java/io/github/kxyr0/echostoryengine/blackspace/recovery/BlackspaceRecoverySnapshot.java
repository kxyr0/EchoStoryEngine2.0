package io.github.kxyr0.echostoryengine.blackspace.recovery;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public record BlackspaceRecoverySnapshot(
        UUID playerId,
        String blackspaceId,
        ResourceLocation originDimension,
        double originX,
        double originY,
        double originZ,
        float yaw,
        float pitch,
        BlackspaceLifecycleState state,
        long createdAtTick
) {
    public BlackspaceRecoverySnapshot {
        playerId = Objects.requireNonNull(playerId, "playerId");
        blackspaceId = requireText(blackspaceId, "blackspaceId");
        originDimension = Objects.requireNonNull(originDimension, "originDimension");
        state = Objects.requireNonNull(state, "state");
    }

    public BlackspaceRecoverySnapshot withState(BlackspaceLifecycleState nextState) {
        return new BlackspaceRecoverySnapshot(playerId, blackspaceId, originDimension, originX, originY, originZ,
                yaw, pitch, nextState, createdAtTick);
    }

    public BlackspaceRecoverySnapshot withBlackspaceIdAndState(String nextBlackspaceId,
            BlackspaceLifecycleState nextState) {
        return new BlackspaceRecoverySnapshot(playerId, nextBlackspaceId, originDimension, originX, originY, originZ,
                yaw, pitch, nextState, createdAtTick);
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checked;
    }
}
