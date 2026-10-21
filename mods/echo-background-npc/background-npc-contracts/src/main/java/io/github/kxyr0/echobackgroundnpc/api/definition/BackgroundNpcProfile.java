package io.github.kxyr0.echobackgroundnpc.api.definition;

import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearancePoolId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundNpcProfileId;
import java.util.Objects;
import java.util.Optional;

public record BackgroundNpcProfile(
        BackgroundNpcProfileId id,
        Optional<BackgroundAppearancePoolId> appearancePool,
        double walkSpeed,
        String locomotionProfile,
        long revision
) {
    public BackgroundNpcProfile {
        id = Objects.requireNonNull(id, "id");
        appearancePool = Objects.requireNonNull(appearancePool, "appearancePool");
        locomotionProfile = Objects.requireNonNull(locomotionProfile, "locomotionProfile").trim();
        if (!Double.isFinite(walkSpeed) || walkSpeed <= 0.0 || revision < 0L) {
            throw new IllegalArgumentException("Invalid profile values.");
        }
    }
}
