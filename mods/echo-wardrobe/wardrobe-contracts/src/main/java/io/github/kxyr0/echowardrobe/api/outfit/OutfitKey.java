package io.github.kxyr0.echowardrobe.api.outfit;

import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.Objects;

/**
 * Canonical exact outfit key in fixed FACE/TOP/BOTTOM/FEET order.
 */
public record OutfitKey(
        WardrobeProfileId profileId,
        WearableSelection face,
        WearableSelection top,
        WearableSelection bottom,
        WearableSelection feet
) {
    public OutfitKey {
        profileId = Objects.requireNonNull(profileId, "profileId");
        face = Objects.requireNonNull(face, "face");
        top = Objects.requireNonNull(top, "top");
        bottom = Objects.requireNonNull(bottom, "bottom");
        feet = Objects.requireNonNull(feet, "feet");
    }

    public static OutfitKey empty(WardrobeProfileId profileId) {
        return new OutfitKey(profileId, WearableSelection.EMPTY, WearableSelection.EMPTY,
                WearableSelection.EMPTY, WearableSelection.EMPTY);
    }

    public WearableSelection selection(WardrobeSlot slot) {
        return switch (slot) {
            case FACE -> face;
            case TOP -> top;
            case BOTTOM -> bottom;
            case FEET -> feet;
        };
    }

    public String serialize() {
        return profileId + "|FACE=" + face.serialize() + "|TOP=" + top.serialize()
                + "|BOTTOM=" + bottom.serialize() + "|FEET=" + feet.serialize();
    }
}
