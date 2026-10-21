package io.github.kxyr0.echonpc.core.definition;

import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionFingerprint;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds deterministic semantic fingerprints for compiled N1 definitions.
 */
public final class NpcDefinitionFingerprinter {
    private NpcDefinitionFingerprinter() {
    }

    public static NpcDefinitionFingerprint humanoid(
            NpcDefinitionId id,
            String displayName,
            Optional<NpcNameColor> nameColor,
            Optional<NpcSkinId> skinId
    ) {
        return humanoid(id, displayName, nameColor, skinId, Optional.empty());
    }

    public static NpcDefinitionFingerprint humanoid(
            NpcDefinitionId id,
            String displayName,
            Optional<NpcNameColor> nameColor,
            Optional<NpcSkinId> skinId,
            Optional<NpcFormId> defaultFormId
    ) {
        nameColor = Objects.requireNonNull(nameColor, "nameColor");
        String skin = skinId.map(NpcSkinId::value).orElse("<none>");
        String form = defaultFormId.map(NpcFormId::value).orElse("<none>");
        String semantic = "v1\narchetype=HUMANOID\nid=" + id.value()
                + "\ndisplay=" + displayName
                + "\nskin=" + skin
                + "\ndefaultForm=" + form + "\n";
        return new NpcDefinitionFingerprint(sha256Hex(semantic));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
