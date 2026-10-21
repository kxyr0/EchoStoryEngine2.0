package io.github.kxyr0.echonpcbackpack.api.payload;

import java.util.Objects;
import java.util.UUID;

/**
 * Neutral exact-identity receipt. The ItemStack NBT stays in the Forge module.
 */
public record NpcBackpackPayloadRef(UUID contentsUuid, String itemId, String fingerprint) {
    public NpcBackpackPayloadRef {
        contentsUuid = Objects.requireNonNull(contentsUuid, "contentsUuid");
        itemId = requireText(itemId, "itemId");
        fingerprint = requireText(fingerprint, "fingerprint");
    }

    private static String requireText(String value, String label) {
        String text = Objects.requireNonNull(value, label).trim();
        if (text.isEmpty() || text.length() > 256) {
            throw new IllegalArgumentException(label + " must be bounded and non-empty.");
        }
        return text;
    }
}
