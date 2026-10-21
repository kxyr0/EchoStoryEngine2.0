package io.github.kxyr0.echoscene.persistence;

import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.SceneExecutionState;
import io.github.kxyr0.echoscene.runtime.SceneCheckpoint;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public final class SceneSnapshotCodec {
    private final SceneDefinitionCodec definitions = new SceneDefinitionCodec();
    public void write(DataOutput output, SavedScene scene) throws IOException {
        output.writeInt(2);
        definitions.write(output, scene.definition());
        output.writeUTF(scene.player().toString());
        output.writeLong(scene.committedTick());
        var checkpoint = scene.checkpoint();
        output.writeUTF(checkpoint.executionId().value());
        output.writeUTF(checkpoint.rootOwner());
        output.writeUTF(checkpoint.state().name());
        output.writeInt(checkpoint.stageIndex());
        output.writeInt(checkpoint.completed().size());
        for (var step : checkpoint.completed().stream().sorted().toList()) output.writeUTF(step.value());
        output.writeInt(checkpoint.pending().size());
        for (var step : checkpoint.pending().stream().sorted().toList()) output.writeUTF(step.value());
        definitions.writeCapabilities(output, checkpoint.held());
        output.writeBoolean(checkpoint.failure().isPresent());
        if (checkpoint.failure().isPresent()) output.writeUTF(checkpoint.failure().get());
        byte[] outbox = scene.eventOutbox().orElse("").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        output.writeInt(outbox.length);
        output.write(outbox);
    }
    public SavedScene read(DataInput input) throws IOException {
        int schema = input.readInt();
        if (schema != 1 && schema != 2) throw new IOException("Unsupported Scene checkpoint schema");
        var definition = definitions.read(input);
        UUID player = UUID.fromString(input.readUTF());
        long tick = input.readLong();
        var id = new SceneExecutionId(input.readUTF());
        String root = input.readUTF();
        var state = SceneExecutionState.valueOf(input.readUTF());
        int stage = input.readInt();
        var completed = new HashSet<SceneStepId>();
        for (int left = SceneDefinitionCodec.count(input); left > 0; left--) completed.add(new SceneStepId(input.readUTF()));
        var pending = new HashSet<SceneStepId>();
        for (int left = SceneDefinitionCodec.count(input); left > 0; left--) pending.add(new SceneStepId(input.readUTF()));
        var held = definitions.readCapabilities(input);
        Optional<String> failure = input.readBoolean() ? Optional.of(input.readUTF()) : Optional.empty();
        Optional<String> outbox = Optional.empty();
        if (schema == 2) {
            int length = input.readInt();
            if (length < 0 || length > 8 * 1024 * 1024) throw new IOException("Invalid Scene event outbox length");
            byte[] bytes = new byte[length];
            input.readFully(bytes);
            if (length > 0) outbox = Optional.of(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }
        if (stage < 0 || stage > definition.stages().size()) throw new IOException("Invalid Scene stage");
        return new SavedScene(definition, new SceneCheckpoint(definition.resourceId(), definition.entry(), definition.revision(),
                id, root, state, stage, completed, pending, held, failure), player, tick, outbox);
    }
}
