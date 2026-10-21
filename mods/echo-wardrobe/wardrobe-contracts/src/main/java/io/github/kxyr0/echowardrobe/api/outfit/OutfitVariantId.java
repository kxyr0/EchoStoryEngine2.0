package io.github.kxyr0.echowardrobe.api.outfit;

import io.github.kxyr0.echowardrobe.api.identity.WardrobeResourceId;

/**
 * Stable identity of an exact outfit variant.
 */
public record OutfitVariantId(String value) {
    public OutfitVariantId {
        value = WardrobeResourceId.require(value, "Outfit variant ID");
    }

    public static OutfitVariantId parse(String raw) {
        return new OutfitVariantId(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
