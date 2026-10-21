package io.github.kxyr0.echowardrobe.core.outfit;

import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitKey;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantId;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantStatus;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.OutfitVariantRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.selection.WearableSelection;

final class OutfitVariantDraft {
    String id;
    String profileId;
    boolean matchSeen;
    WearableSelection face = WearableSelection.EMPTY;
    WearableSelection top = WearableSelection.EMPTY;
    WearableSelection bottom = WearableSelection.EMPTY;
    WearableSelection feet = WearableSelection.EMPTY;
    String worldSkin;
    String faceSkin;

    OutfitVariantDefinition toDefinition(OutfitVariantStatus status, WearableSourceFingerprint fingerprint) {
        WardrobeProfileId profile = WardrobeProfileId.parse(profileId);
        OutfitKey key = new OutfitKey(profile, face, top, bottom, feet);
        return new OutfitVariantDefinition(OutfitVariantId.parse(id), profile, key, worldSkin, faceSkin, status,
                new OutfitVariantRevision(0L), fingerprint);
    }

    static WearableSelection selection(String raw) {
        return raw == null ? WearableSelection.EMPTY : WearableSelection.of(WearableId.parse(raw));
    }
}
