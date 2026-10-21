package io.github.kxyr0.echonpc.api.form.definition;

import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import java.util.Objects;
import java.util.Optional;

public record NpcAppearanceDefinition(
        Optional<NpcSkinId> skinId,
        String modelProfile,
        String animationGraphId,
        String faceProfileId,
        String portraitResourceId,
        NpcAppearanceRevision revision
) {
    public NpcAppearanceDefinition {
        skinId = Objects.requireNonNull(skinId, "skinId");
        modelProfile = text(modelProfile, "modelProfile");
        animationGraphId = text(animationGraphId, "animationGraphId");
        faceProfileId = text(faceProfileId, "faceProfileId");
        portraitResourceId = text(portraitResourceId, "portraitResourceId");
        revision = Objects.requireNonNull(revision, "revision");
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 128) {
            throw new IllegalArgumentException(name + " must be non-empty and bounded.");
        }
        return checked;
    }
}
