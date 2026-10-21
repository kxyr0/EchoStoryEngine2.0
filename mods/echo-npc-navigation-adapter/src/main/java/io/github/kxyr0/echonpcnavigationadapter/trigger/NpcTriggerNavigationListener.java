package io.github.kxyr0.echonpcnavigationadapter.trigger;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackAction;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackInvocation;
import io.github.kxyr0.echotrigger.forge.api.callback.TriggerCallbackInvocationEvent;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationPayloads;
import io.github.kxyr0.echonpcnavigationadapter.script.NpcNavigationScriptFacade;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class NpcTriggerNavigationListener {
    private final NpcNavigationScriptFacade facade = new NpcNavigationScriptFacade();

    @SubscribeEvent
    public void onTriggerCallback(TriggerCallbackInvocationEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        int index = 0;
        for (TriggerCallbackAction action : event.invocation().actions()) {
            dispatch(server, event.invocation(), action, index);
            index++;
        }
    }

    private void dispatch(
            MinecraftServer server,
            TriggerCallbackInvocation invocation,
            TriggerCallbackAction action,
            int index
    ) {
        try {
            if (action.name().equals("Npc.Move") || action.name().equals("Npc.Sprint")) {
                var command = action.name().equals("Npc.Sprint")
                        ? NpcNavigationPayloads.sprint(action.arguments())
                        : NpcNavigationPayloads.move(action.arguments());
                facade.move(server, ownerExecutionId(invocation, index), command);
            } else if (action.name().equals("Npc.Stop")) {
                facade.stop(server, NpcNavigationPayloads.actor(action.arguments()));
            }
        } catch (RuntimeException exception) {
            // Trigger callbacks are fire-and-forget; invalid NPC navigation payloads are ignored by this adapter.
        }
    }

    private String ownerExecutionId(TriggerCallbackInvocation invocation, int index) {
        return "trigger_" + invocation.instanceId().value() + "_"
                + invocation.kind().name().toLowerCase(Locale.ROOT) + "_"
                + invocation.serverTick() + "_" + index;
    }
}
