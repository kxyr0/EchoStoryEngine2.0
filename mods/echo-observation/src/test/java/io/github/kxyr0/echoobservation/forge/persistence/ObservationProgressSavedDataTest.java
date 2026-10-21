package io.github.kxyr0.echoobservation.forge.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoobservation.api.action.ObservationActionExecutionId;
import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.core.progress.ObservationProgressBook;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class ObservationProgressSavedDataTest {
    @Test
    void completionActivationAndActionReceiptSurviveRoundTrip() {
        UUID player = UUID.randomUUID();
        ObservationResourceId resource = ObservationResourceId.parse("jdh:museum_room");
        ObservationId observation = ObservationId.parse("jdh:museum_room#broken_window");
        ObservationCompletionId completion = ObservationCompletionId.deterministic(player, observation);
        ObservationActionExecutionId receipt = ObservationActionExecutionId.of(completion, 0);
        ObservationProgressBook book = new ObservationProgressBook();
        var progress = book.player(player);
        progress.activate(resource);
        progress.disable(observation);
        progress.complete(observation, completion);
        progress.acknowledge(receipt);
        CompoundTag encoded = new ObservationProgressSavedData(book).save(new CompoundTag());

        var restored = ObservationProgressSavedData.fromTag(encoded).book().player(player);
        assertTrue(restored.active(resource));
        assertTrue(restored.completed(observation));
        assertTrue(restored.receipt(receipt));
        assertEquals(completion, restored.completion(observation).orElseThrow());
    }
}
