package io.github.kxyr0.echoplayer.forge.api.appearance.resolved;

import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import io.github.kxyr0.echoplayer.api.identity.EchoPlayerId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Final public appearance after neutral contributors are applied.
 */
public record ResolvedPlayerAppearance(
        EchoPlayerId playerId,
        String worldSkin,
        String faceSkin,
        PlayerSkinModel skinModel,
        Optional<String> sourceId,
        List<WearableModelDescriptor> wearableModels,
        PlayerAppearanceRevision baseRevision,
        PlayerAppearanceRevision contributionRevision,
        PlayerAppearanceRevision finalRevision
) {
    public ResolvedPlayerAppearance {
        playerId = Objects.requireNonNull(playerId, "playerId");
        worldSkin = Objects.requireNonNull(worldSkin, "worldSkin");
        faceSkin = Objects.requireNonNull(faceSkin, "faceSkin");
        skinModel = Objects.requireNonNull(skinModel, "skinModel");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        wearableModels = List.copyOf(wearableModels);
        baseRevision = Objects.requireNonNull(baseRevision, "baseRevision");
        contributionRevision = Objects.requireNonNull(contributionRevision, "contributionRevision");
        finalRevision = Objects.requireNonNull(finalRevision, "finalRevision");
    }

    public String renderKey() {
        return playerId + "|" + worldSkin + "|" + faceSkin + "|" + finalRevision.value();
    }
}
