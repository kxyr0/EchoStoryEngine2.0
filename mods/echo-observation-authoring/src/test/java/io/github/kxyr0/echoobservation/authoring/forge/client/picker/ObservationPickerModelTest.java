package io.github.kxyr0.echoobservation.authoring.forge.client.picker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ObservationPickerModelTest {
    @Test
    void ordersResourcesThenUnplacedThenId() {
        ObservationPickerModel model = new ObservationPickerModel(List.of(
                item("z:r", "z:r#b", true, "Placed"),
                item("a:r", "a:r#z", true, "PlacedA"),
                item("z:r", "z:r#a", false, "Draft"),
                item("a:r", "a:r#a", false, "DraftA")), "z:r#b");
        assertEquals(List.of("a:r#a", "a:r#z", "z:r#a", "z:r#b"),
                model.visible().stream().map(ObservationPickerItem::observationId).toList());
    }

    @Test
    void searchesAllAuthoringIdentitiesCaseInsensitively() {
        ObservationPickerModel model = new ObservationPickerModel(List.of(
                new ObservationPickerItem("jdh:home", "jdh:home#draft", "BrokenClock",
                        "data/jdh/observations/home.es", false)), "");
        model.search("BROKENCLOCK");
        assertEquals(1, model.visible().size());
        model.search("HOME.ES");
        assertEquals(1, model.visible().size());
        model.search("missing");
        assertEquals(0, model.visible().size());
    }

    private static ObservationPickerItem item(String resource, String id, boolean placed, String declaration) {
        return new ObservationPickerItem(resource, id, declaration, resource + ".es", placed);
    }
}
