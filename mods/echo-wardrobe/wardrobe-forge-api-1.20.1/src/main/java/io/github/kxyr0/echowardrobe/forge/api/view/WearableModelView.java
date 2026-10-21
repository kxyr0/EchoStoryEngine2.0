package io.github.kxyr0.echowardrobe.forge.api.view;

import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import java.util.List;
import java.util.UUID;

/**
 * Query-only wearable model contribution view.
 */
public interface WearableModelView {
    List<WearableModelContribution> wearableModels(UUID playerUuid);
}
