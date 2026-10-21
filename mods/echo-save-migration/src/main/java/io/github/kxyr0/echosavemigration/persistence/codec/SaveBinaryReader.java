package io.github.kxyr0.echosavemigration.persistence.codec;

import io.github.kxyr0.echosavemigration.api.EchoSaveManifest;
import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.MigrationReceipt;
import io.github.kxyr0.echosavemigration.api.NodeId;
import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.api.StateValue;
import io.github.kxyr0.echosavemigration.data.SaveSnapshot;
import java.io.DataInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SaveBinaryReader {
    SaveSnapshot read(DataInputStream input) throws IOException {
        if (!input.readUTF().equals("ECHO_SAVE_1")) throw new IOException("Unsupported Echo save format.");
        SaveSchemaVersion schema = new SaveSchemaVersion(input.readInt());
        long revision = input.readLong();
        String fingerprint = input.readUTF();
        EchoSaveManifest manifest = new EchoSaveManifest(schema, revision, fingerprint, stringIntegers(input));
        Map<String, ResourceId> resources = new LinkedHashMap<>();
        repeat(input, () -> resources.put(input.readUTF(), new ResourceId(input.readUTF())));
        Map<ResourceId, NodeId> dialogue = nodes(input);
        Map<ResourceId, NodeId> scenes = nodes(input);
        Map<ResourceId, StateValue> states = new LinkedHashMap<>();
        repeat(input, () -> states.put(new ResourceId(input.readUTF()), state(input)));
        List<MigrationReceipt> receipts = new ArrayList<>();
        repeat(input, () -> receipts.add(receipt(input)));
        return new SaveSnapshot(manifest, resources, dialogue, scenes, states, receipts);
    }

    private Map<String, Integer> stringIntegers(DataInputStream input) throws IOException {
        Map<String, Integer> values = new LinkedHashMap<>();
        repeat(input, () -> values.put(input.readUTF(), input.readInt()));
        return values;
    }

    private Map<ResourceId, NodeId> nodes(DataInputStream input) throws IOException {
        Map<ResourceId, NodeId> values = new LinkedHashMap<>();
        repeat(input, () -> values.put(new ResourceId(input.readUTF()), new NodeId(input.readUTF())));
        return values;
    }

    private StateValue state(DataInputStream input) throws IOException {
        return switch (input.readByte()) {
            case 1 -> new StateValue.BoolState(input.readBoolean());
            case 2 -> new StateValue.IntState(input.readInt());
            case 3 -> new StateValue.StringState(input.readUTF());
            case 4 -> new StateValue.ResourceState(new ResourceId(input.readUTF()));
            case 5 -> new StateValue.NodeState(new NodeId(input.readUTF()));
            default -> throw new IOException("Unknown typed state value.");
        };
    }

    private MigrationReceipt receipt(DataInputStream input) throws IOException {
        return new MigrationReceipt(new MigrationId(input.readUTF()), new SaveSchemaVersion(input.readInt()),
                new SaveSchemaVersion(input.readInt()), input.readUTF(), input.readLong(),
                Instant.ofEpochMilli(input.readLong()));
    }

    private void repeat(DataInputStream input, IoAction action) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > 1_000_000) throw new IOException("Invalid collection size: " + count);
        for (int index = 0; index < count; index++) action.run();
    }

    @FunctionalInterface
    private interface IoAction { void run() throws IOException; }
}
