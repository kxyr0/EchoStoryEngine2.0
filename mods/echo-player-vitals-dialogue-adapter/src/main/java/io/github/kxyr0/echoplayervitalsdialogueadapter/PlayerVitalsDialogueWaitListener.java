package io.github.kxyr0.echoplayervitalsdialogueadapter;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echoplayervitals.api.mutation.PlayerVitalsMutationId;
import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsResult;
import io.github.kxyr0.echoplayervitals.api.snapshot.PlayerVitalsSnapshot;
import io.github.kxyr0.echoplayervitals.forge.api.EchoPlayerVitalsService;
import io.github.kxyr0.echoplayervitals.forge.api.EchoPlayerVitalsServiceLookupEvent;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public final class PlayerVitalsDialogueWaitListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onExternalWait(DialogueExternalWaitDispatchEvent event) {
        if (!PlayerVitalsDialogueCommands.supported(event.externalWait().waitType())
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
            LOGGER.warn("ECHO-VITALS-DIALOGUE-NO-DIALOGUE-SERVICE session={}",
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
            EchoPlayerVitalsService vitals = vitals(player.server).orElseThrow(() ->
                    new IllegalStateException("Echo Player Vitals service is unavailable."));
            PlayerVitalsResult<PlayerVitalsSnapshot> result = apply(vitals, player, wait);
            if (!result.successful()) {
                throw new IllegalStateException("Echo Player Vitals rejected fatigue dialogue command.");
            }
            dialogue.resumeExternal(new DialogueExternalResumeRequest(session.sessionId(),
                    session.revision(), wait.waitKey()));
        } catch (RuntimeException exception) {
            LOGGER.warn("ECHO-VITALS-DIALOGUE-FAILED session={} type={}",
                    session.sessionId().value(), wait.waitType(), exception);
            dialogue.cancel(session.sessionId());
        }
    }

    private PlayerVitalsResult<PlayerVitalsSnapshot> apply(
            EchoPlayerVitalsService vitals,
            ServerPlayer player,
            DialogueExternalWait wait
    ) {
        Map<String, String> payload = DialogueCommandPayload.decode(wait.payload());
        return switch (wait.waitType()) {
            case PlayerVitalsDialogueCommands.SET_FATIGUE ->
                    vitals.setFatigue(player.getUUID(), mutationId(wait),
                            rawFatigueForReserve(vitals, player, value(payload)));
            case PlayerVitalsDialogueCommands.ADD_FATIGUE ->
                    vitals.addFatigue(player.getUUID(), mutationId(wait), value(payload));
            case PlayerVitalsDialogueCommands.REDUCE_FATIGUE ->
                    vitals.reduceFatigue(player.getUUID(), mutationId(wait), value(payload));
            case PlayerVitalsDialogueCommands.RESTORE_FATIGUE ->
                    vitals.restoreFatigue(player.getUUID(), mutationId(wait));
            default -> throw new IllegalArgumentException("Unsupported wait type: " + wait.waitType());
        };
    }

    private PlayerVitalsMutationId mutationId(DialogueExternalWait wait) {
        String source = wait.waitType() + "|" + wait.waitKey();
        UUID id = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
        return new PlayerVitalsMutationId("dialogue:" + id);
    }

    private int value(Map<String, String> payload) {
        String raw = payload.get("value");
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Fatigue dialogue command requires value.");
        }
        int value = Integer.parseInt(raw);
        if (value < 0) {
            throw new IllegalArgumentException("Fatigue dialogue command value cannot be negative.");
        }
        return value;
    }

    private int rawFatigueForReserve(EchoPlayerVitalsService vitals, ServerPlayer player, int reserve) {
        int maximum = vitals.findByAccountUuid(player.getUUID())
                .map(snapshot -> snapshot.fatigue().fatigue().maximum())
                .orElse(100);
        return PlayerVitalsReserveMath.rawFatigueForReserve(reserve, maximum);
    }

    private Optional<DialogueService> dialogue(MinecraftServer server) {
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }

    private Optional<EchoPlayerVitalsService> vitals(MinecraftServer server) {
        EchoPlayerVitalsServiceLookupEvent event = new EchoPlayerVitalsServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.providers().stream().findFirst();
    }
}
