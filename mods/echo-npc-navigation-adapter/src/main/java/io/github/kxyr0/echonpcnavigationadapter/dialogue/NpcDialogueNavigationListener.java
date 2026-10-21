package io.github.kxyr0.echonpcnavigationadapter.dialogue;

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

public final class NpcDialogueNavigationListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOVE = "echo_navigation:npc_move";
    private static final String MOVE_ARRIVAL = "echo_navigation:npc_move_arrival";
    private static final String STOP = "echo_navigation:npc_stop";
    private static final String SET_FORM = "echo_npc:set_form";
    private static final String CHANGE_SKIN = "echo_npc:change_skin";
    private static final String SLEEP_AT = "echo_npc:sleep_at";
    private static final String WAKE_UP = "echo_npc:wake_up";

    private final DialogueNavigationWaits waits;

    public NpcDialogueNavigationListener(DialogueNavigationWaits waits) {
        this.waits = waits;
    }

    @SubscribeEvent
    public void onExternalWait(DialogueExternalWaitDispatchEvent event) {
        if (!supported(event.externalWait().waitType())) {
            return;
        }
        LOGGER.info("ENPC-NAV-DIALOGUE-WAIT type={} session={} revision={}",
                event.externalWait().waitType(), event.session().sessionId().value(), event.session().revision());
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("ENPC-NAV-DIALOGUE-NO-SERVER session={}", event.session().sessionId().value());
            return;
        }
        service(server).ifPresentOrElse(service -> dispatch(server, service, event),
                () -> LOGGER.warn("ENPC-NAV-DIALOGUE-NO-SERVICE session={}", event.session().sessionId().value()));
    }

    private void dispatch(MinecraftServer server, DialogueService service, DialogueExternalWaitDispatchEvent event) {
        if (event.externalWait().waitType().equals(MOVE)) {
            waits.move(server, service, event.session(), event.externalWait());
        } else if (event.externalWait().waitType().equals(MOVE_ARRIVAL)) {
            waits.moveArrival(server, service, event.session(), event.externalWait());
        } else if (event.externalWait().waitType().equals(STOP)) {
            waits.stop(server, service, event.session(), event.externalWait());
        } else if (event.externalWait().waitType().equals(SET_FORM)) {
            waits.setForm(server, service, event.session(), event.externalWait());
        } else if (event.externalWait().waitType().equals(CHANGE_SKIN)) {
            waits.changeSkin(server, service, event.session(), event.externalWait());
        } else if (event.externalWait().waitType().equals(SLEEP_AT)) {
            waits.sleepAt(server, service, event.session(), event.externalWait());
        } else if (event.externalWait().waitType().equals(WAKE_UP)) {
            waits.wakeUp(server, service, event.session(), event.externalWait());
        }

    }

    private Optional<DialogueService> service(MinecraftServer server) {
        EchoDialogueServiceLookupEvent lookup = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        return lookup.providers().stream().findFirst();
    }

    private boolean supported(String waitType) {
        return waitType.equals(MOVE) || waitType.equals(MOVE_ARRIVAL) || waitType.equals(STOP)
                || waitType.equals(SET_FORM) || waitType.equals(CHANGE_SKIN) || waitType.equals(SLEEP_AT) || waitType.equals(WAKE_UP);
    }
}

