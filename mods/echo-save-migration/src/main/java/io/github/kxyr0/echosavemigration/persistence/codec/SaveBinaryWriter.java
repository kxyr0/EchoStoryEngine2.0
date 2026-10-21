package io.github.kxyr0.echosavemigration.persistence.codec;

import io.github.kxyr0.echosavemigration.api.MigrationReceipt;
import io.github.kxyr0.echosavemigration.api.NodeId;
import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.api.StateValue;
import io.github.kxyr0.echosavemigration.data.SaveSnapshot;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

final class SaveBinaryWriter {
    void write(DataOutputStream output, SaveSnapshot snapshot) throws IOException {
        output.writeUTF("ECHO_SAVE_1");
        output.writeInt(snapshot.manifest().schema().value());
        output.writeLong(snapshot.manifest().migrationRevision());
        output.writeUTF(snapshot.manifest().contentFingerprint());
        stringIntegers(output, snapshot.manifest().moduleVersions());
        output.writeInt(snapshot.resourceReferences().size());
        for (var entry : snapshot.resourceReferences().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            output.writeUTF(entry.getKey());
            output.writeUTF(entry.getValue().value());
        }
        nodes(output, snapshot.dialogueNodes());
        nodes(output, snapshot.sceneNodes());
        output.writeInt(snapshot.states().size());
        for (var entry : snapshot.states().entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            output.writeUTF(entry.getKey().value());
            state(output, entry.getValue());
        }
        output.writeInt(snapshot.receipts().size());
        for (MigrationReceipt receipt : snapshot.receipts()) receipt(output, receipt);
    }

    private void stringIntegers(DataOutputStream output, Map<String, Integer> values) throws IOException {
        output.writeInt(values.size());
        for (var entry : values.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            output.writeUTF(entry.getKey());
            output.writeInt(entry.getValue());
        }
    }

    private void nodes(DataOutputStream output, Map<ResourceId, NodeId> values) throws IOException {
        output.writeInt(values.size());
        for (var entry : values.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            output.writeUTF(entry.getKey().value());
            output.writeUTF(entry.getValue().value());
        }
    }

    private void state(DataOutputStream output, StateValue value) throws IOException {
        if (value instanceof StateValue.BoolState bool) { output.writeByte(1); output.writeBoolean(bool.value()); }
        else if (value instanceof StateValue.IntState integer) { output.writeByte(2); output.writeInt(integer.value()); }
        else if (value instanceof StateValue.StringState text) { output.writeByte(3); output.writeUTF(text.value()); }
        else if (value instanceof StateValue.ResourceState resource) { output.writeByte(4); output.writeUTF(resource.value().value()); }
        else if (value instanceof StateValue.NodeState node) { output.writeByte(5); output.writeUTF(node.value().value()); }
        else throw new IOException("Unknown state value type.");
    }

    private void receipt(DataOutputStream output, MigrationReceipt value) throws IOException {
        output.writeUTF(value.migrationId().value());
        output.writeInt(value.from().value());
        output.writeInt(value.to().value());
        output.writeUTF(value.contentFingerprint());
        output.writeLong(value.revision());
        output.writeLong(value.appliedAt().toEpochMilli());
    }
}
