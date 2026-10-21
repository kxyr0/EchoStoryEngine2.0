package io.github.kxyr0.echoeffectsdialogueadapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echoeffects.api.identity.EffectCommandId;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.forge.api.EffectService;
import io.github.kxyr0.echoeffects.forge.api.lookup.EchoEffectServiceLookupEvent;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public final class EffectsDialogueWaitListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onExternalWait(DialogueExternalWaitDispatchEvent event) {
        if (!EffectsDialogueCommands.supported(event.externalWait().waitType())
                || event.player().isEmpty()) {
            return;
        }
        ServerPlayer player = event.player().orElseThrow();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Optional<DialogueService> dialogue = dialogue(server);
        if (dialogue.isEmpty()) {
            LOGGER.warn("ECHO-EFFECTS-DIALOGUE-NO-DIALOGUE-SERVICE session={}",
                    event.session().sessionId().value());
            return;
        }
        handle(dialogue.orElseThrow(), player, event.session(), event.externalWait());
    }

    private void handle(
            DialogueService dialogue,
            ServerPlayer player,
            DialogueSessionView session,
            DialogueExternalWait wait
    ) {
        try {
            EffectService effects = effects(player.getServer()).orElseThrow(() ->
                    new IllegalStateException("Echo Effects service is unavailable."));
            apply(effects, player, session, wait);
            dialogue.resumeExternal(new DialogueExternalResumeRequest(session.sessionId(),
                    session.revision(), wait.waitKey()));
        } catch (RuntimeException exception) {
            LOGGER.warn("ECHO-EFFECTS-DIALOGUE-FAILED session={} type={}",
                    session.sessionId().value(), wait.waitType(), exception);
            dialogue.cancel(session.sessionId());
        }
    }

    private void apply(
            EffectService effects,
            ServerPlayer player,
            DialogueSessionView session,
            DialogueExternalWait wait
    ) {
        Map<String, String> payload = DialogueCommandPayload.decode(wait.payload());
        EffectId effectId = EffectId.of(required(payload, "effectId"));
        double intensity = Double.parseDouble(payload.getOrDefault("intensity", "1.0"));
        if (effects.applyToTarget(EffectTargetRef.player(player.getUUID()), effectId,
                commandId(session, wait, effectId), intensity).isEmpty()) {
            throw new IllegalStateException("Echo Effects rejected dialogue command.");
        }
    }

    private EffectCommandId commandId(DialogueSessionView session, DialogueExternalWait wait, EffectId effectId) {
        return EffectCommandId.deterministic("dialogue", session.sessionId().value(), wait.waitKey(), effectId);
    }

    private String required(Map<String, String> payload, String key) {
        String value = payload.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Effect dialogue command requires " + key + ".");
        }
        return value;
    }

    private Optional<DialogueService> dialogue(MinecraftServer server) {
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }

    private Optional<EffectService> effects(MinecraftServer server) {
        EchoEffectServiceLookupEvent event = new EchoEffectServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }
}
