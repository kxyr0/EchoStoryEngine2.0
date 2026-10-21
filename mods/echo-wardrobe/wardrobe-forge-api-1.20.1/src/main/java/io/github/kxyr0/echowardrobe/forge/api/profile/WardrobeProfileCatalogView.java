package io.github.kxyr0.echowardrobe.forge.api.profile;

import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileId;
import java.util.List;
import java.util.Optional;

/**
 * Query-only wardrobe profile view.
 */
public interface WardrobeProfileCatalogView {
    int wardrobeProfileCount();

    List<WardrobeProfileDefinition> wardrobeProfiles();

    Optional<WardrobeProfileDefinition> wardrobeProfile(WardrobeProfileId id);
}
