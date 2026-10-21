package io.github.kxyr0.echonpc.api.form.definition;

import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyAnchorSnapshot;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyDimensions;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyGeometry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record NpcFormSnapshot(
        NpcFormId id,
        NpcFormRevision revision,
        Optional<io.github.kxyr0.echonpc.api.identity.NpcSkinId> skinId,
        String modelProfile,
        NpcBodyGeometry bodyGeometry,
        NpcBodyDimensions dimensions,
        List<NpcBodyAnchorSnapshot> anchors,
        String animationGraphId,
        String faceProfileId,
        String portraitResourceId,
        String fingerprint
) {
    public NpcFormSnapshot {
        id = Objects.requireNonNull(id, "id");
        revision = Objects.requireNonNull(revision, "revision");
        skinId = Objects.requireNonNull(skinId, "skinId");
        modelProfile = text(modelProfile, "modelProfile");
        bodyGeometry = Objects.requireNonNull(bodyGeometry, "bodyGeometry");
        dimensions = Objects.requireNonNull(dimensions, "dimensions");
        anchors = List.copyOf(Objects.requireNonNull(anchors, "anchors"));
        animationGraphId = text(animationGraphId, "animationGraphId");
        faceProfileId = text(faceProfileId, "faceProfileId");
        portraitResourceId = text(portraitResourceId, "portraitResourceId");
        fingerprint = sha(fingerprint);
    }

    private static String text(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty() || checked.length() > 128) {
            throw new IllegalArgumentException(name + " must be non-empty and bounded.");
        }
        return checked;
    }

    private static String sha(String value) {
        String checked = Objects.requireNonNull(value, "fingerprint").trim().toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("fingerprint must be SHA-256 hex.");
        }
        return checked;
    }
}
