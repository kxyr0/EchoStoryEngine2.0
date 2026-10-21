package io.github.kxyr0.echoplayer.core.skin;

import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinId;
import io.github.kxyr0.echoplayer.api.appearance.PlayerSkinModel;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Validated custom resource skin candidate.
 */
public record SkinCandidate(PlayerSkinId skinId, PlayerSkinModel model, Path path, String fingerprint) {
    public SkinCandidate {
        skinId = Objects.requireNonNull(skinId, "skinId");
        model = Objects.requireNonNull(model, "model");
        path = Objects.requireNonNull(path, "path");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }
}
