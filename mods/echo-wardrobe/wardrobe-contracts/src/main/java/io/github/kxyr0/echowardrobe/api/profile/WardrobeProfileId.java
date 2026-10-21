package io.github.kxyr0.echowardrobe.api.profile;

import io.github.kxyr0.echowardrobe.api.identity.WardrobeResourceId;

/**
 * Stable identity of a wardrobe profile.
 */
public record WardrobeProfileId(String value) {
    public WardrobeProfileId {
        value = WardrobeResourceId.require(value, "Wardrobe profile ID");
    }

    public static WardrobeProfileId parse(String raw) {
        return new WardrobeProfileId(raw);
    }

    @Override
    public String toString() {
        return value;
    }
}
