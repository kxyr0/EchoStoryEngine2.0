package io.github.kxyr0.echoitem.core.manifest;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.ItemKindId;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisteredItemManifestTest {
    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void startupIncludesManifestOnlyMissingDefinition() {
        EchoItemId id = EchoItemId.parse("jdh:old_sword");
        var manifest = new RegisteredItemManifest(List.of(record(id)));

        var plan = new ItemRegistrationPlanner().startup(manifest, List.of());

        assertEquals(List.of(id), plan.registeredIds());
        assertEquals(List.of(id), plan.missingDefinitionPlaceholders());
    }

    @Test
    void liveNewItemBecomesPendingRestart() {
        EchoItemId id = EchoItemId.parse("jdh:new_sword");

        var plan = new ItemRegistrationPlanner().live(RegisteredItemManifest.empty(), List.of(), List.of(id));

        assertEquals(id, plan.pendingRestartIds().get(0).id());
        assertTrue(plan.pendingRestartIds().get(0).message().contains("after restart"));
    }

    @Test
    void liveExistingPendingManifestRecordRemainsPendingRestart() {
        EchoItemId id = EchoItemId.parse("jdh:new_sword");
        var manifest = new RegisteredItemManifest(List.of(record(id).pending()));

        var plan = new ItemRegistrationPlanner().live(manifest, List.of(), List.of());

        assertEquals(id, plan.pendingRestartIds().get(0).id());
    }

    @Test
    void manifestWritesAndReadsAtomically() throws Exception {
        EchoItemId id = EchoItemId.parse("jdh:void_sword");
        RegisteredItemManifestStore store = new RegisteredItemManifestStore(tempDir);

        store.write(new RegisteredItemManifest(List.of(record(id))));

        assertTrue(Files.isRegularFile(store.path()));
        assertEquals(List.of(id), store.load().ids());
    }

    private RegisteredItemRecord record(EchoItemId id) {
        return new RegisteredItemRecord(id, ItemKindId.WEAPON, "test",
                new ItemSourceFingerprint("0".repeat(64)), false, false);
    }
}
