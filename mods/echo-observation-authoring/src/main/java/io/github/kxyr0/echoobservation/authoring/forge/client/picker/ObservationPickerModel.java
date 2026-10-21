package io.github.kxyr0.echoobservation.authoring.forge.client.picker;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ObservationPickerModel {
    private static final Comparator<ObservationPickerItem> ORDER = Comparator
            .comparing(ObservationPickerItem::resourceId)
            .thenComparing(ObservationPickerItem::placed)
            .thenComparing(ObservationPickerItem::observationId);
    private final List<ObservationPickerItem> source;
    private List<ObservationPickerItem> visible;
    private String draftId;

    public ObservationPickerModel(List<ObservationPickerItem> items, String selectedId) {
        source = items.stream().sorted(ORDER).toList();
        visible = source;
        draftId = source.stream().anyMatch(item -> item.observationId().equals(selectedId))
                ? selectedId : source.stream().findFirst().map(ObservationPickerItem::observationId).orElse(null);
    }

    public void search(String query) {
        visible = source.stream().filter(item -> item.matches(query)).toList();
        if (visible.stream().noneMatch(item -> item.observationId().equals(draftId))) {
            draftId = visible.stream().findFirst().map(ObservationPickerItem::observationId).orElse(null);
        }
    }

    public void move(int delta) {
        if (visible.isEmpty()) return;
        int index = java.util.stream.IntStream.range(0, visible.size())
                .filter(value -> visible.get(value).observationId().equals(draftId)).findFirst().orElse(0);
        draftId = visible.get(Math.floorMod(index + delta, visible.size())).observationId();
    }

    public void select(String id) {
        if (visible.stream().anyMatch(item -> item.observationId().equals(id))) draftId = id;
    }

    public List<ObservationPickerItem> visible() { return visible; }
    public Optional<ObservationPickerItem> selected() {
        return visible.stream().filter(item -> item.observationId().equals(draftId)).findFirst();
    }
}

