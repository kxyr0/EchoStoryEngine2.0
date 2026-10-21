package io.github.kxyr0.echonpcbackpack.script.trigger;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackCommandResult;
import io.github.kxyr0.echonpcbackpack.script.NpcBackpackScriptFacade;
import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackAction;
import io.github.kxyr0.echotrigger.forge.api.callback.TriggerCallbackInvocationEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

public final class NpcBackpackTriggerListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onCallback(TriggerCallbackInvocationEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        NpcBackpackScriptFacade facade = new NpcBackpackScriptFacade(server);
        for (int index = 0; index < event.invocation().actions().size(); index++) {
            TriggerCallbackAction action = event.invocation().actions().get(index);
            String owner = event.invocation().instanceId().value() + ":"
                    + event.invocation().kind().name().toLowerCase(java.util.Locale.ROOT) + ":"
                    + event.invocation().serverTick() + ":" + index;
            try {
                NpcBackpackCommandResult result = dispatch(facade, owner, action);
                if (result != null) {
                    LOGGER.info("NPC_BACKPACK_TRIGGER_DISPATCH commandId={} outcome={} diagnostic={}",
                            result.commandId().value(), result.outcome(), result.diagnosticCode());
                }
            } catch (RuntimeException exception) {
                LOGGER.error("NPC_BACKPACK_TRIGGER_DISPATCH_FAILED trigger={} action={}",
                        event.invocation().triggerId().value(), action.name(), exception);
            }
        }
    }

    private NpcBackpackCommandResult dispatch(NpcBackpackScriptFacade facade, String owner,
            TriggerCallbackAction action) {
        return switch (action.name()) {
            case "Npc.EquipBackpack" -> facade.equip(owner, action.arguments());
            case "Npc.UnequipBackpack" -> facade.unequip(owner, action.arguments());
            default -> null;
        };
    }
}
