package io.github.kxyr0.echowardrobe.core.equipment;

/**
 * Plain Java view over item availability.
 */
public interface ItemRegistryView {
    boolean itemExists(String itemId);

    static ItemRegistryView allKnown() {
        return itemId -> true;
    }

    static ItemRegistryView noneKnown() {
        return itemId -> false;
    }
}
