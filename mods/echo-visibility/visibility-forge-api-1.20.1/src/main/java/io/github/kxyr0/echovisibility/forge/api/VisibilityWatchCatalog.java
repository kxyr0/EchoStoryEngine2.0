package io.github.kxyr0.echovisibility.forge.api;

import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import java.util.List;
import java.util.Optional;

public interface VisibilityWatchCatalog {
    Optional<VisibilityWatchDefinition> find(VisibilityWatchId watchId);

    List<VisibilityWatchDefinition> all();

    long catalogRevision();
}
