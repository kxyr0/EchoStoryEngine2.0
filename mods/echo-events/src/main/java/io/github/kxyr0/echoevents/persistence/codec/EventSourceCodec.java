package io.github.kxyr0.echoevents.persistence.codec;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class EventSourceCodec {
    public void write(DataOutput output, EventSource source) throws IOException {
        output.writeUTF(source.resource().value());
        output.writeUTF(source.execution().value());
        output.writeUTF(source.rootExecution().value());
        output.writeUTF(source.transition().value());
        output.writeBoolean(source.audience() instanceof EventAudience.Player);
        if (source.audience() instanceof EventAudience.Player player) output.writeUTF(player.playerId().toString());
        output.writeUTF(source.module());
        output.writeLong(source.gameTick());
        output.writeBoolean(source.echoTimeMicros().isPresent());
        if (source.echoTimeMicros().isPresent()) output.writeLong(source.echoTimeMicros().orElseThrow());
        output.writeUTF(source.definitionRevision());
        output.writeBoolean(source.origin().isPresent());
        if (source.origin().isPresent()) writeOrigin(output, source.origin().orElseThrow());
        output.writeBoolean(source.callerExecution().isPresent());
        if (source.callerExecution().isPresent()) output.writeUTF(source.callerExecution().get().value());
    }
    public EventSource read(DataInput input) throws IOException {
        var resource = new ResourceId(input.readUTF());
        var execution = new ExecutionId(input.readUTF());
        var root = new ExecutionId(input.readUTF());
        var transition = new TransitionId(input.readUTF());
        EventAudience audience = input.readBoolean() ? new EventAudience.Player(UUID.fromString(input.readUTF()))
                : new EventAudience.World();
        String module = input.readUTF();
        long tick = input.readLong();
        OptionalLong time = input.readBoolean() ? OptionalLong.of(input.readLong()) : OptionalLong.empty();
        String revision = input.readUTF();
        Optional<EventSource.Origin> origin = input.readBoolean() ? Optional.of(readOrigin(input)) : Optional.empty();
        Optional<ExecutionId> caller = input.readBoolean() ? Optional.of(new ExecutionId(input.readUTF())) : Optional.empty();
        return new EventSource(resource, execution, root, transition, audience, module, tick, time, revision, origin, caller);
    }
    private void writeOrigin(DataOutput output, EventSource.Origin origin) throws IOException {
        output.writeUTF(origin.sourceId());
        output.writeInt(origin.line());
        output.writeInt(origin.column());
        output.writeBoolean(origin.caller().isPresent());
        if (origin.caller().isPresent()) output.writeUTF(origin.caller().orElseThrow().value());
    }
    private EventSource.Origin readOrigin(DataInput input) throws IOException {
        String source = input.readUTF();
        int line = input.readInt();
        int column = input.readInt();
        Optional<ExecutionId> caller = input.readBoolean() ? Optional.of(new ExecutionId(input.readUTF())) : Optional.empty();
        return new EventSource.Origin(source, line, column, caller);
    }
}
