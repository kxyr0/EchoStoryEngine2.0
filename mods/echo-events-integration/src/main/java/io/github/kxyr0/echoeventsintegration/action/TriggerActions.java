package io.github.kxyr0.echoeventsintegration.action;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.delivery.EventReactionPort.Acceptance;
import io.github.kxyr0.echotrigger.forge.api.domain.TriggerEventActionDispatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TriggerActions {
    private final StoryTriggerAction stories = new StoryTriggerAction();
    @SubscribeEvent
    public void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { stories.clear(); }
    @SubscribeEvent
    public void dispatch(TriggerEventActionDispatchEvent event) {
        event.resolveSourceResource(new io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId(
                ActionResources.source(event)));
        if (!(event.delivery().event().source().audience() instanceof EventAudience.Player player)) {
            event.resolve(Acceptance.REJECTED);
            return;
        }
        var target = event.server().getPlayerList().getPlayer(player.playerId());
        if (target == null) { event.resolve(Acceptance.WAITING_CONTEXT); return; }
        var acceptance = switch (event.action().kind()) {
            case SCENE_PLAY -> new SceneTriggerAction().start(event, target);
            case DIALOGUE_PLAY -> new DialogueTriggerAction().start(event, target);
            case OBSERVATION_ACTIVATE, OBSERVATION_ENABLE -> new ObservationTriggerAction().start(event, target);
            case STORY_START -> stories.start(event, target);
        };
        event.resolve(acceptance);
    }
}
