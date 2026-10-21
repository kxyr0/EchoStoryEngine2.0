package io.github.kxyr0.echowardrobe.api.identity;

/**
 * Typed logical ID of a wardrobe wearable definition.
 */
public record WearableId(String value) {
    public WearableId {
        value = WardrobeResourceId.require(value, "Wearable ID");
    }

    public static WearableId parse(String raw) {
        return new WearableId(raw);
    }

    public String namespace() {
        return value.substring(0, value.indexOf(':'));
    }

    public String resourcePath() {
        return value.substring(value.indexOf(':') + 1);
    }

    @Override
    public String toString() {
        return value;
    }
}
