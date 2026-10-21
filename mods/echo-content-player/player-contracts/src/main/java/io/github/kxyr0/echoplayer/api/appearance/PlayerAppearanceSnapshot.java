package io.github.kxyr0.echoplayer.api.appearance;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable base appearance snapshot for server authority and client rendering.
 */
public record PlayerAppearanceSnapshot(
        PlayerSkinMode skinMode,
        Optional<PlayerSkinId> skinId,
        Optional<PlayerSkinModel> skinModel,
        String sourceFingerprint,
        long appearanceRevision
) {
    public PlayerAppearanceSnapshot {
        skinMode = Objects.requireNonNull(skinMode, "skinMode");
        skinId = Objects.requireNonNull(skinId, "skinId");
        skinModel = Objects.requireNonNull(skinModel, "skinModel");
        sourceFingerprint = Objects.requireNonNull(sourceFingerprint, "sourceFingerprint");
        if (skinMode == PlayerSkinMode.RESOURCE && (skinId.isEmpty() || skinModel.isEmpty())) {
            throw new IllegalArgumentException("RESOURCE skin mode requires skin ID and model.");
        }
        if (skinMode == PlayerSkinMode.ACCOUNT && skinId.isPresent()) {
            throw new IllegalArgumentException("ACCOUNT skin mode must not store an authored skin ID.");
        }
        if (appearanceRevision < 0L) {
            throw new IllegalArgumentException("Appearance revision must not be negative.");
        }
    }

    public static PlayerAppearanceSnapshot account(long revision) {
        return new PlayerAppearanceSnapshot(PlayerSkinMode.ACCOUNT, Optional.empty(), Optional.empty(), "", revision);
    }

    public static PlayerAppearanceSnapshot resource(PlayerSkinId id, PlayerSkinModel model,
                                                    String fingerprint, long revision) {
        return new PlayerAppearanceSnapshot(PlayerSkinMode.RESOURCE, Optional.of(id), Optional.of(model),
                fingerprint, revision);
    }
}
