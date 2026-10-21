package io.github.kxyr0.echoscene.persistence;

import io.github.kxyr0.echoscene.api.definition.SceneCapability;
import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.api.definition.SceneCommand;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.definition.SceneStage;
import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.BlockingMode;
import io.github.kxyr0.echoscene.api.model.SceneRevision;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SceneDefinitionCodec {
    public void write(DataOutput output, SceneDefinition definition) throws IOException {
        output.writeUTF(definition.resourceId().value());
        output.writeUTF(definition.entry().value());
        output.writeLong(definition.revision().value());
        output.writeInt(definition.stages().size());
        for (var stage : definition.stages()) {
            output.writeInt(stage.index());
            output.writeInt(stage.commands().size());
            for (var command : stage.commands()) writeCommand(output, command);
        }
    }
    public SceneDefinition read(DataInput input) throws IOException {
        var resource = new SceneResourceId(input.readUTF());
        var entry = new SceneLocalId(input.readUTF());
        var revision = new SceneRevision(input.readLong());
        var stages = new ArrayList<SceneStage>();
        for (int remaining = count(input); remaining > 0; remaining--) {
            int index = input.readInt();
            var commands = new ArrayList<SceneCommand>();
            for (int left = count(input); left > 0; left--) commands.add(readCommand(input));
            stages.add(new SceneStage(index, commands));
        }
        return new SceneDefinition(resource, entry, revision, stages);
    }
    private void writeCommand(DataOutput output, SceneCommand command) throws IOException {
        output.writeUTF(command.stepId().value());
        output.writeUTF(command.receiver());
        output.writeUTF(command.operation());
        output.writeInt(command.arguments().size());
        for (String argument : command.arguments()) output.writeUTF(argument);
        output.writeUTF(command.blockingMode().name());
        writeCapabilities(output, command.capabilities());
        var span = command.span();
        output.writeUTF(span.sourceId().value());
        output.writeInt(span.startOffset());
        output.writeInt(span.endOffset());
        output.writeInt(span.start().line());
        output.writeInt(span.start().column());
        output.writeInt(span.end().line());
        output.writeInt(span.end().column());
    }
    private SceneCommand readCommand(DataInput input) throws IOException {
        var step = new SceneStepId(input.readUTF());
        String receiver = input.readUTF();
        String operation = input.readUTF();
        var arguments = new ArrayList<String>();
        for (int left = count(input); left > 0; left--) arguments.add(input.readUTF());
        var blocking = BlockingMode.valueOf(input.readUTF());
        var capabilities = readCapabilities(input);
        var source = new SourceId(input.readUTF());
        int start = input.readInt();
        int end = input.readInt();
        var span = new SourceSpan(source, start, end, new SourcePosition(start, input.readInt(), input.readInt()),
                new SourcePosition(end, input.readInt(), input.readInt()));
        return new SceneCommand(step, receiver, operation, arguments, blocking, capabilities, span);
    }
    public void writeCapabilities(DataOutput output, List<SceneCapability> capabilities) throws IOException {
        output.writeInt(capabilities.size());
        for (var capability : capabilities) {
            output.writeUTF(capability.kind().name());
            output.writeUTF(capability.resource());
        }
    }
    public List<SceneCapability> readCapabilities(DataInput input) throws IOException {
        var capabilities = new ArrayList<SceneCapability>();
        for (int left = count(input); left > 0; left--) capabilities.add(new SceneCapability(
                SceneCapabilityKind.valueOf(input.readUTF()), input.readUTF()));
        return List.copyOf(capabilities);
    }
    public static int count(DataInput input) throws IOException {
        int size = input.readInt();
        if (size < 0 || size > 100_000) throw new IOException("Invalid Scene collection size");
        return size;
    }
}
