package io.github.kxyr0.echonpc.core.definition;

import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionFingerprint;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable compiled N1 NPC definition. It deliberately contains presentation metadata only.
 */
public record NpcDefinition(
        NpcDefinitionId id,
        String displayName,
        Optional<NpcNameColor> nameColor,
        Optional<NpcSkinId> skinId,
        Optional<NpcFormId> defaultFormId,
        NpcDefinitionFingerprint fingerprint,
        NpcArchetype archetype
) {
    public NpcDefinition {
        id = Objects.requireNonNull(id, "id");
        displayName = requireDisplayName(displayName);
        nameColor = Objects.requireNonNull(nameColor, "nameColor");
        skinId = Objects.requireNonNull(skinId, "skinId");
        defaultFormId = Objects.requireNonNull(defaultFormId, "defaultFormId");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        archetype = Objects.requireNonNull(archetype, "archetype");
    }

    public NpcDefinition(NpcDefinitionId id, String displayName, Optional<NpcNameColor> nameColor,
            Optional<NpcSkinId> skinId, NpcDefinitionFingerprint fingerprint, NpcArchetype archetype) {
        this(id, displayName, nameColor, skinId, Optional.empty(), fingerprint, archetype);
    }

    public static NpcDefinition humanoid(NpcDefinitionId id, String displayName, Optional<NpcSkinId> skinId) {
        return humanoid(id, displayName, Optional.empty(), skinId);
    }

    public static NpcDefinition humanoid(
            NpcDefinitionId id,
            String displayName,
            Optional<NpcNameColor> nameColor,
            Optional<NpcSkinId> skinId
    ) {
        return humanoid(id, displayName, nameColor, skinId, Optional.empty());
    }

    public static NpcDefinition humanoid(
            NpcDefinitionId id,
            String displayName,
            Optional<NpcNameColor> nameColor,
            Optional<NpcSkinId> skinId,
            Optional<NpcFormId> defaultFormId
    ) {
        NpcDefinitionFingerprint fingerprint = NpcDefinitionFingerprinter.humanoid(id, displayName, nameColor,
                skinId, defaultFormId);
        return new NpcDefinition(id, displayName, nameColor, skinId, defaultFormId, fingerprint,
                NpcArchetype.HUMANOID);
    }

    private static String requireDisplayName(String value) {
        String checked = Objects.requireNonNull(value, "displayName");
        if (checked.isBlank() || checked.length() > 64) {
            throw new IllegalArgumentException("displayName must be non-empty and at most 64 characters.");
        }
        return checked;
    }
}
