package io.github.kxyr0.echoeventsintegration.action;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventExecutionContext;
import io.github.kxyr0.echoevents.api.delivery.EventReactionPort.Acceptance;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import io.github.kxyr0.echotrigger.forge.api.domain.TriggerEventActionDispatchEvent;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

final class StoryTriggerAction {
    private final Map<String, Acceptance> pending = new HashMap<>();
    void clear() { pending.clear(); }
    Acceptance start(TriggerEventActionDispatchEvent action, ServerPlayer player) {
        String token = action.actionToken();
        var previous = pending.get(token);
        if (previous != null) {
            if (previous != Acceptance.WAITING_CONTEXT) pending.remove(token);
            return previous;
        }
        var service = EchoStoryExecutionBridge.lookup(action.server()).starter();
        if (service.isEmpty()) return Acceptance.RETRY;
        String id = "echo_events:trigger/" + UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8))
                + "/player/" + player.getUUID();
        var execution = new io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId(id);
        var source = action.delivery().event().source();
        EchoEvents.service(action.server()).rememberContexts(Map.of(execution, new EventExecutionContext(
                source.rootExecution(), source.execution(), new EventAudience.Player(player.getUUID()),
                Optional.of(action.action().target()))));
        pending.put(token, Acceptance.WAITING_CONTEXT);
        service.get().startDurable(StoryId.of(ActionResources.target(action)), ExecutionId.of(id),
                committed -> pending.put(token, committed ? Acceptance.ACCEPTED : Acceptance.RETRY));
        return Acceptance.WAITING_CONTEXT;
    }
}
