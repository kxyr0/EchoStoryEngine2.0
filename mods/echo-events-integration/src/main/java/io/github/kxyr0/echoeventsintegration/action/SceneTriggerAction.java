package io.github.kxyr0.echoeventsintegration.action;

import io.github.kxyr0.echoevents.api.delivery.EventReactionPort.Acceptance;
import io.github.kxyr0.echoscene.api.identity.SceneExecutionId;
import io.github.kxyr0.echoscene.forge.api.EchoSceneLaunchEvent;
import io.github.kxyr0.echotrigger.forge.api.domain.TriggerEventActionDispatchEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;

final class SceneTriggerAction {
    Acceptance start(TriggerEventActionDispatchEvent action, ServerPlayer player) {
        var event = new EchoSceneLaunchEvent(player, ActionResources.target(action),
                new SceneExecutionId(action.actionToken()), action.delivery().event().source().rootExecution().value());
        MinecraftForge.EVENT_BUS.post(event);
        return event.result().isPresent() ? Acceptance.ACCEPTED : Acceptance.REJECTED;
    }
}
