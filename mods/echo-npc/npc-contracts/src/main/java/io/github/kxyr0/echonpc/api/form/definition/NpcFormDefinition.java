package io.github.kxyr0.echonpc.api.form.definition;

import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyAnchorDefinition;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyDimensions;
import io.github.kxyr0.echonpc.api.form.body.NpcBodyGeometry;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record NpcFormDefinition(
        NpcFormId id,
        NpcAppearanceDefinition appearance,
        NpcBodyGeometry bodyGeometry,
        NpcBodyDimensions dimensions,
        List<NpcBodyAnchorDefinition> anchors,
        Set<String> tags,
        NpcFormRevision revision,
        String sourceLocation,
        String fingerprint
) implements Comparable<NpcFormDefinition> {
    public NpcFormDefinition {
        id = Objects.requireNonNull(id, "id");
        appearance = Objects.requireNonNull(appearance, "appearance");
        bodyGeometry = Objects.requireNonNull(bodyGeometry, "bodyGeometry");
        dimensions = Objects.requireNonNull(dimensions, "dimensions");
        anchors = List.copyOf(Objects.requireNonNull(anchors, "anchors"));
        tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
        revision = Objects.requireNonNull(revision, "revision");
        sourceLocation = Objects.requireNonNull(sourceLocation, "sourceLocation").trim();
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint").trim().toLowerCase(java.util.Locale.ROOT);
        if (sourceLocation.isEmpty() || !fingerprint.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("NpcFormDefinition source/fingerprint is invalid.");
        }
    }

    public NpcFormSnapshot snapshot() {
        return new NpcFormSnapshot(id, revision, appearance.skinId(), appearance.modelProfile(), bodyGeometry,
                dimensions, anchors.stream().map(NpcBodyAnchorDefinition::snapshot).toList(),
                appearance.animationGraphId(), appearance.faceProfileId(), appearance.portraitResourceId(),
                fingerprint);
    }

    @Override
    public int compareTo(NpcFormDefinition other) {
        return id.compareTo(other.id);
    }
}
