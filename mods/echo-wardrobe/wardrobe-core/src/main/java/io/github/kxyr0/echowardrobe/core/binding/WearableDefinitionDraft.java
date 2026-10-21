package io.github.kxyr0.echowardrobe.core.binding;

import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import io.github.kxyr0.echowardrobe.api.contribution.WearableModelTransform;
import io.github.kxyr0.echowardrobe.api.contribution.WearablePortraitOverlayContribution;
import io.github.kxyr0.echowardrobe.api.contribution.WearablePortraitStrip;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinitionStatus;
import io.github.kxyr0.echowardrobe.api.definition.WearableItemBinding;
import io.github.kxyr0.echowardrobe.api.identity.WearableId;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import io.github.kxyr0.echowardrobe.api.revision.WearableDefinitionRevision;
import io.github.kxyr0.echowardrobe.api.revision.WearableSourceFingerprint;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeEquipmentSlot;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class WearableDefinitionDraft {
    String id;
    String profileId;
    String itemId;
    WardrobeSlot slot;
    WardrobeEquipmentSlot equipmentSlot;
    String collection = "UNSPECIFIED";
    String modelId;
    double[] position = new double[] {0D, 0D, 0D};
    double[] rotation = new double[] {0D, 0D, 0D};
    double[] scale = new double[] {1D, 1D, 1D};
    String portraitTextureId;
    List<WearablePortraitStrip> portraitStrips = new ArrayList<>();

    WearableDefinition toDefinition(boolean itemResolved, WearableSourceFingerprint fingerprint) {
        WearableDefinitionStatus status = itemResolved ? WearableDefinitionStatus.ACTIVE
                : WearableDefinitionStatus.UNRESOLVED_ITEM;
        return new WearableDefinition(WearableId.parse(id), WardrobeProfileId.parse(profileId), slot, collection,
                new WearableItemBinding(itemId, equipmentSlot), Optional.ofNullable(modelContribution()),
                Optional.ofNullable(portraitContribution()), status, new WearableDefinitionRevision(0L), fingerprint);
    }

    private WearableModelContribution modelContribution() {
        if (modelId == null) {
            return null;
        }
        WearableModelTransform transform = new WearableModelTransform(position[0], position[1], position[2],
                rotation[0], rotation[1], rotation[2], scale[0], scale[1], scale[2]);
        return new WearableModelContribution(modelId, transform);
    }

    private WearablePortraitOverlayContribution portraitContribution() {
        if (portraitTextureId == null && portraitStrips.isEmpty()) {
            return null;
        }
        if (portraitTextureId == null || portraitStrips.isEmpty()) {
            throw new IllegalArgumentException("Portrait overlay requires PortraitTexture and PortraitStrip.");
        }
        return new WearablePortraitOverlayContribution(portraitTextureId, portraitStrips);
    }
}
