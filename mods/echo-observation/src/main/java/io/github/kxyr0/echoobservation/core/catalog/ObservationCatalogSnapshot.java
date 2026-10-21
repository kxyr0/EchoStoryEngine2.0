package io.github.kxyr0.echoobservation.core.catalog;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.api.model.ObservationResourceDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ObservationCatalogSnapshot {
    private final long revision;
    private final Map<ObservationResourceId, ObservationResourceDefinition> resources;
    private final Map<ObservationId, ObservationDefinition> observations;

    public ObservationCatalogSnapshot(long revision, Collection<ObservationResourceDefinition> resources) {
        if (revision < 0L) throw new IllegalArgumentException("Catalog revision must not be negative.");
        this.revision = revision;
        Map<ObservationResourceId, ObservationResourceDefinition> resourceMap = new LinkedHashMap<>();
        Map<ObservationId, ObservationDefinition> observationMap = new LinkedHashMap<>();
        Objects.requireNonNull(resources, "resources").stream().sorted((left, right) -> left.id().compareTo(right.id()))
                .forEach(resource -> {
                    if (resourceMap.put(resource.id(), resource) != null) {
                        throw new IllegalArgumentException("Duplicate Observation resource: " + resource.id());
                    }
                    resource.observations().forEach(value -> {
                        if (observationMap.put(value.id(), value) != null) {
                            throw new IllegalArgumentException("Duplicate Observation: " + value.id());
                        }
                    });
                });
        this.resources = Map.copyOf(resourceMap);
        this.observations = Map.copyOf(observationMap);
    }

    public static ObservationCatalogSnapshot empty() {
        return new ObservationCatalogSnapshot(0L, List.of());
    }

    public long revision() { return revision; }
    public Collection<ObservationResourceDefinition> resources() { return resources.values(); }
    public Optional<ObservationResourceDefinition> resource(ObservationResourceId id) {
        return Optional.ofNullable(resources.get(id));
    }
    public Optional<ObservationDefinition> observation(ObservationId id) {
        return Optional.ofNullable(observations.get(id));
    }
    public int observationCount() { return observations.size(); }
}
