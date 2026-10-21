package io.github.kxyr0.echoobservation.authoring.forge.client.picker;

import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import java.util.Locale;
import java.util.Objects;

public record ObservationPickerItem(
        String resourceId,
        String observationId,
        String declaration,
        String sourceId,
        boolean placed) {
    public ObservationPickerItem {
        resourceId = Objects.requireNonNull(resourceId, "resourceId");
        observationId = Objects.requireNonNull(observationId, "observationId");
        declaration = Objects.requireNonNull(declaration, "declaration");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
    }

    public static ObservationPickerItem from(ObservationDefinition definition) {
        return new ObservationPickerItem(definition.id().resource().toString(), definition.id().toString(),
                definition.symbol(), definition.source().sourceId(), definition.placed());
    }

    public boolean matches(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        return resourceId.toLowerCase(Locale.ROOT).contains(needle)
                || observationId.toLowerCase(Locale.ROOT).contains(needle)
                || declaration.toLowerCase(Locale.ROOT).contains(needle)
                || sourceId.toLowerCase(Locale.ROOT).contains(needle);
    }
}

