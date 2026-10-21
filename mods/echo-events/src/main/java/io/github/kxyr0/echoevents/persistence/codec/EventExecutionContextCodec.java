package io.github.kxyr0.echoevents.persistence.codec;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventExecutionContext;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

final class EventExecutionContextCodec {
    void write(DataOutput output, EventExecutionContext context) throws IOException {
        output.writeUTF(context.root().value());
        output.writeUTF(context.caller().value());
        output.writeBoolean(context.audience() instanceof EventAudience.Player);
        if (context.audience() instanceof EventAudience.Player player) output.writeUTF(player.playerId().toString());
        output.writeBoolean(context.targetResource().isPresent());
        if (context.targetResource().isPresent()) output.writeUTF(context.targetResource().get().value());
    }
    EventExecutionContext read(DataInput input) throws IOException {
        var root = new ExecutionId(input.readUTF());
        var caller = new ExecutionId(input.readUTF());
        EventAudience audience = input.readBoolean() ? new EventAudience.Player(UUID.fromString(input.readUTF()))
                : new EventAudience.World();
        Optional<ResourceId> target = input.readBoolean() ? Optional.of(new ResourceId(input.readUTF())) : Optional.empty();
        return new EventExecutionContext(root, caller, audience, target);
    }
}
