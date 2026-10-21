package io.github.kxyr0.echoobservation.forge.persistence;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationLocalId;
import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalogSnapshot;

public final class ObservationProgressMigration {
    public boolean apply(ObservationCatalogSnapshot catalog, ObservationProgressSavedData data) {
        boolean changed = false;
        for (var definition : catalog.resources().stream()
                .flatMap(resource -> resource.observations().stream()).toList()) {
            for (String former : definition.formerLocalIds()) {
                try {
                    ObservationId oldId = new ObservationId(definition.id().resource(),
                            new ObservationLocalId(former));
                    for (var progress : data.book().entries().values()) {
                        changed |= progress.migrate(oldId, definition.id());
                    }
                } catch (IllegalArgumentException ignored) {
                    // Invalid legacy aliases cannot match a persisted typed ID.
                }
            }
        }
        if (changed) data.setDirty();
        return changed;
    }
}
