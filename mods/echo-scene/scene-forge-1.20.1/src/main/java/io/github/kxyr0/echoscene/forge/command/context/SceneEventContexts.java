package io.github.kxyr0.echoscene.forge.command.context;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventExecutionContext;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import io.github.kxyr0.echoscene.api.command.SceneCommandRequest;
import java.util.LinkedHashMap;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class SceneEventContexts {
    private SceneEventContexts() { }
    public static void capture(SceneCommandRequest request, ServerPlayer player, String... identities) {
        var context = new EventExecutionContext(new ExecutionId(request.rootOwner()),
                new ExecutionId(request.executionId().value()), new EventAudience.Player(player.getUUID()),
                Optional.of(new ResourceId(request.command().arguments().get(0))));
        var entries = new LinkedHashMap<ExecutionId, EventExecutionContext>();
        for (String id : identities) entries.put(new ExecutionId(id), context);
        EchoEvents.service(player.getServer()).rememberContexts(entries);
    }
}
