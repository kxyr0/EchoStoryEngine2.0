package io.github.kxyr0.echonpcinteraction.script.trigger;

import io.github.kxyr0.echonpcinteraction.script.NpcInteractionScriptFacade;
import io.github.kxyr0.echotrigger.forge.api.callback.TriggerCallbackInvocationEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class NpcInteractionTriggerListener {
    private final NpcInteractionScriptFacade facade = new NpcInteractionScriptFacade();

    @SubscribeEvent
    public void onCallback(TriggerCallbackInvocationEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        event.invocation().actions().forEach(action -> {
            String owner = event.invocation().instanceId().value() + ":" + event.invocation().serverTick();
            if (action.name().equals("Npc.Interact")) {
                facade.interact(server, owner, action.arguments());
            } else if (action.name().equals("Npc.StopInteraction")) {
                facade.stop(server, owner, action.arguments());
            } else if (action.name().equals("Npc.StandUp")) {
                facade.standUp(server, owner, action.arguments());
            }
        });
    }
}
