package io.github.kxyr0.echoevents;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.payload.EventPayload.TeleportFact;
import io.github.kxyr0.echoevents.persistence.codec.EventPayloadCodec;
import java.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeleportEventTest {
    @Test void allFiveLifecycleFactsRoundTripThroughCanonicalJournalCodec() throws Exception {
        var codec = new EventPayloadCodec();
        for (var kind : EventKind.values()) {
            if (!kind.name().startsWith("TELEPORT_")) continue;
            var expected = new TeleportFact(kind, new SymbolId("jdh:nolan"));
            var bytes = new ByteArrayOutputStream();
            codec.write(new DataOutputStream(bytes), expected);
            assertEquals(expected, codec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        }
    }
    @Test void unrelatedEventKindCannotMasqueradeAsTeleport() {
        assertThrows(IllegalArgumentException.class, () -> new TeleportFact(EventKind.NPC_DIED, new SymbolId("nolan")));
    }
}
