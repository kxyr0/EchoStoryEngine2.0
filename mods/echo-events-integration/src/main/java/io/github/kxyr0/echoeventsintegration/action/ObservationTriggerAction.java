package io.github.kxyr0.echoeventsintegration.action;

import io.github.kxyr0.echoevents.api.delivery.EventReactionPort.Acceptance;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import io.github.kxyr0.echoobservation.api.runtime.ObservationServiceBridge;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import io.github.kxyr0.echotrigger.forge.api.domain.TriggerEventActionDispatchEvent;
import net.minecraft.server.level.ServerPlayer;

final class ObservationTriggerAction {
    Acceptance start(TriggerEventActionDispatchEvent action, ServerPlayer player) {
        var service = ObservationServiceBridge.lookup(action.server());
        if (service.isEmpty()) return Acceptance.RETRY;
        String target = ActionResources.target(action);
        var result = action.action().kind() == EventTriggerDefinition.Kind.OBSERVATION_ACTIVATE
                ? service.get().activate(player.getUUID(), ObservationResourceId.parse(target))
                : service.get().enable(player.getUUID(), ObservationId.parse(target));
        return result.successful() ? Acceptance.ACCEPTED : Acceptance.REJECTED;
    }
}
