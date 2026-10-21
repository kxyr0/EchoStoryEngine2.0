package io.github.kxyr0.echonpc.api.service;

import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionFingerprint;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded public summary of one compiled NPC definition.
 */
public record NpcDefinitionSummary(
        NpcDefinitionId definitionId,
        String displayName,
        Optional<NpcNameColor> nameColor,
        Optional<NpcSkinId> skinId,
        Optional<NpcFormId> defaultFormId,
        NpcDefinitionFingerprint fingerprint
) implements Comparable<NpcDefinitionSummary> {
    public NpcDefinitionSummary {
        definitionId = Objects.requireNonNull(definitionId, "definitionId");
        displayName = Objects.requireNonNull(displayName, "displayName");
        nameColor = Objects.requireNonNull(nameColor, "nameColor");
        skinId = Objects.requireNonNull(skinId, "skinId");
        defaultFormId = Objects.requireNonNull(defaultFormId, "defaultFormId");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
    }

    public NpcDefinitionSummary(
            NpcDefinitionId definitionId,
            String displayName,
            Optional<NpcSkinId> skinId,
            NpcDefinitionFingerprint fingerprint
    ) {
        this(definitionId, displayName, Optional.empty(), skinId, Optional.empty(), fingerprint);
    }

    @Override
    public int compareTo(NpcDefinitionSummary other) {
        return definitionId.compareTo(other.definitionId);
    }
}
