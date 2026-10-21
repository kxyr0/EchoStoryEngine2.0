package io.github.kxyr0.echowardrobe.forge.api.model;

import io.github.kxyr0.echowardrobe.api.contribution.WearableModelContribution;
import java.util.Optional;

/**
 * Optional boundary for callers that can provide item model descriptors.
 */
public interface WearableModelProvider {
    Optional<WearableModelContribution> model(String modelResourceId);
}
