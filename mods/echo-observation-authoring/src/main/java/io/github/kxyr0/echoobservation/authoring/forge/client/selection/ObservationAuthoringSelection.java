package io.github.kxyr0.echoobservation.authoring.forge.client.selection;

import io.github.kxyr0.echoobservation.api.client.ObservationClientView;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import io.github.kxyr0.echoobservation.api.model.ObservationResourceDefinition;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ObservationAuthoringSelection {
    private Optional<ObservationId> selected = Optional.empty();

    public Optional<ObservationDefinition> definition(ObservationClientView view) {
        Optional<ObservationDefinition> current = selected.flatMap(view.catalog()::observation);
        if (current.isPresent()) return current;
        Optional<ObservationDefinition> initial = observations(view).stream().filter(value -> !value.placed()).findFirst()
                .or(() -> observations(view).stream().findFirst());
        selected = initial.map(ObservationDefinition::id);
        return initial;
    }

    public void cycleObservation(ObservationClientView view, int direction) {
        List<ObservationDefinition> values = observations(view);
        if (values.isEmpty()) { selected = Optional.empty(); return; }
        int index = definition(view).map(values::indexOf).orElse(0);
        selected = Optional.of(values.get(Math.floorMod(index + direction, values.size())).id());
    }

    public void cycleResource(ObservationClientView view, int direction) {
        List<ObservationResourceDefinition> resources = view.catalog().resources().stream()
                .sorted(Comparator.comparing(ObservationResourceDefinition::id)).toList();
        if (resources.isEmpty()) { selected = Optional.empty(); return; }
        var resourceId = definition(view).map(value -> value.id().resource()).orElse(resources.get(0).id());
        int index = java.util.stream.IntStream.range(0, resources.size())
                .filter(value -> resources.get(value).id().equals(resourceId)).findFirst().orElse(0);
        var resource = resources.get(Math.floorMod(index + direction, resources.size()));
        selected = resource.observations().stream().sorted(preferred()).findFirst().map(ObservationDefinition::id);
    }

    public void select(ObservationId id) { selected = Optional.of(id); }
    public Optional<ObservationId> selectedId() { return selected; }

    public boolean advanceUnplacedInResource(ObservationClientView view) {
        Optional<ObservationDefinition> current = definition(view);
        if (current.isEmpty()) return false;
        List<ObservationDefinition> values = view.catalog().resource(current.orElseThrow().id().resource())
                .map(resource -> resource.observations().stream().filter(value -> !value.placed())
                        .sorted(Comparator.comparing(ObservationDefinition::id)).toList()).orElse(List.of());
        if (values.isEmpty()) return false;
        String currentId = current.orElseThrow().id().toString();
        selected = values.stream().filter(value -> value.id().toString().compareTo(currentId) > 0).findFirst()
                .or(() -> values.stream().findFirst()).map(ObservationDefinition::id);
        return true;
    }

    private List<ObservationDefinition> observations(ObservationClientView view) {
        return view.catalog().resources().stream().flatMap(value -> value.observations().stream())
                .sorted(Comparator.comparing(ObservationDefinition::id)).toList();
    }

    private Comparator<ObservationDefinition> preferred() {
        return Comparator.comparing(ObservationDefinition::placed).thenComparing(ObservationDefinition::id);
    }
}
