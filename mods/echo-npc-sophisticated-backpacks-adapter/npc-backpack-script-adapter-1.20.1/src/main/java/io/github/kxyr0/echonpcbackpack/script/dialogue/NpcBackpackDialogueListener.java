package io.github.kxyr0.echonpcbackpack.script.dialogue;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

public final class NpcBackpackDialogueListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String EQUIP = "echo_npc_sophisticated_backpacks:equip";
    private static final String UNEQUIP = "echo_npc_sophisticated_backpacks:unequip";
    private final DialogueBackpackWaits waits;

    public NpcBackpackDialogueListener(DialogueBackpackWaits waits) {
        this.waits = waits;
    }

    @SubscribeEvent
    public void onExternalWait(DialogueExternalWaitDispatchEvent event) {
        String type = event.externalWait().waitType();
        if (!type.equals(EQUIP) && !type.equals(UNEQUIP)) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        service(server).ifPresent(service -> dispatch(server, service, event, type.equals(EQUIP)));
    }

    private void dispatch(MinecraftServer server, DialogueService service,
            DialogueExternalWaitDispatchEvent event, boolean equip) {
        try {
            waits.dispatch(server, service, event.session(), event.externalWait(), equip);
        } catch (RuntimeException exception) {
            LOGGER.error("NPC_BACKPACK_DIALOGUE_WAIT_FAILED session={} waitType={} waitKey={}",
                    event.session().sessionId().value(), event.externalWait().waitType(),
                    event.externalWait().waitKey(), exception);
            service.cancel(event.session().sessionId());
        }
    }

    private Optional<DialogueService> service(MinecraftServer server) {
        EchoDialogueServiceLookupEvent lookup = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.providers().stream().findFirst();
    }
}
