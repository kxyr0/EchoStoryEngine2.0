package io.github.kxyr0.echonpcinteraction.script.dialogue;

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

public final class NpcInteractionDialogueListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String INTERACT = "echo_npc_interaction:interact";
    private static final String STOP = "echo_npc_interaction:stop";
    private static final String STAND_UP = "echo_npc_interaction:stand_up";
    private final DialogueInteractionWaits waits;

    public NpcInteractionDialogueListener(DialogueInteractionWaits waits) {
        this.waits = waits;
    }

    @SubscribeEvent
    public void onExternalWait(DialogueExternalWaitDispatchEvent event) {
        if (!supported(event.externalWait().waitType())) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        service(server).ifPresent(service -> dispatchSafely(server, service, event));
    }

    private void dispatchSafely(MinecraftServer server, DialogueService service,
            DialogueExternalWaitDispatchEvent event) {
        try {
            dispatch(server, service, event);
        } catch (RuntimeException exception) {
            LOGGER.error("NPC_INTERACTION_DIALOGUE_WAIT_FAILED session={} waitType={} waitKey={}",
                    event.session().sessionId().value(), event.externalWait().waitType(),
                    event.externalWait().waitKey(), exception);
            service.cancel(event.session().sessionId());
        }
    }

    private void dispatch(MinecraftServer server, DialogueService service, DialogueExternalWaitDispatchEvent event) {
        if (event.externalWait().waitType().equals(INTERACT)) {
            waits.interact(server, service, event.session(), event.externalWait());
        } else if (event.externalWait().waitType().equals(STOP)) {
            waits.stop(server, service, event.session(), event.externalWait());
        } else if (event.externalWait().waitType().equals(STAND_UP)) {
            waits.standUp(server, service, event.session(), event.externalWait());
        }
    }

    private Optional<DialogueService> service(MinecraftServer server) {
        EchoDialogueServiceLookupEvent lookup = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.providers().stream().findFirst();
    }

    private boolean supported(String waitType) {
        return waitType.equals(INTERACT) || waitType.equals(STOP) || waitType.equals(STAND_UP);
    }
}
