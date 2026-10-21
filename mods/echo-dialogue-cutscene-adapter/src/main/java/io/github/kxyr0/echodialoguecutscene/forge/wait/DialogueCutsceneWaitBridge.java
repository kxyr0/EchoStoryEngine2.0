package io.github.kxyr0.echodialoguecutscene.forge.wait;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.forge.api.event.CutsceneLifecycleEvent;
import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public final class DialogueCutsceneWaitBridge {
    private final DialogueCutsceneServices services;
    private final PendingCutsceneWaits pending = new PendingCutsceneWaits();
    private final Logger logger;

    public DialogueCutsceneWaitBridge(DialogueCutsceneServices services, Logger logger) {
        this.services = services;
        this.logger = logger;
    }

    @SubscribeEvent
    public void onDialogueExternalWait(DialogueExternalWaitDispatchEvent event) {
        Optional<CutsceneWaitType> type = CutsceneWaitType.from(event.externalWait().waitType());
        if (type.isEmpty() || event.player().isEmpty()) {
            return;
        }
        ServerPlayer player = event.player().orElseThrow();
        services.dialogue(player.getServer()).flatMap(dialogue -> services.cutscene(player.getServer())
                .map(cutscene -> new ServicePair(dialogue, cutscene)))
                .ifPresent(pair -> handle(pair, player, event.session(), event.externalWait(), type.orElseThrow()));
    }

    @SubscribeEvent
    public void onCutsceneLifecycle(CutsceneLifecycleEvent event) {
        MinecraftServer server = event.player().getServer();
        if (server == null) {
            return;
        }
        services.dialogue(server).flatMap(dialogue -> services.cutscene(server)
                .map(cutscene -> new ServicePair(dialogue, cutscene)))
                .ifPresent(pair -> resumeReady(event.player(), pair));
    }

    private void handle(
            ServicePair services,
            ServerPlayer player,
            DialogueSessionView session,
            DialogueExternalWait wait,
            CutsceneWaitType type
    ) {
        Optional<CutsceneId> cutsceneId = cutsceneId(wait);
        if (cutsceneId.isEmpty()) {
            logger.warn("Cutscene dialogue command is missing cutscene id: {}", wait.waitKey());
            resume(services.dialogue(), session, wait);
            return;
        }
        switch (type) {
            case START -> startAndResume(services, player, session, wait, cutsceneId.orElseThrow());
            case PLAY -> startAndWait(services, player, session, wait, cutsceneId.orElseThrow());
            case WAIT_PREPARED -> waitUntilPrepared(services, player, session, wait, cutsceneId.orElseThrow());
            case WAIT -> waitUntilDone(services, player, session, wait, cutsceneId.orElseThrow());
            case STOP -> stopAndResume(services, player, session, wait, cutsceneId.orElseThrow());
        }
    }

    private void startAndResume(ServicePair services, ServerPlayer player, DialogueSessionView session,
            DialogueExternalWait wait, CutsceneId cutsceneId) {
        services.cutscene().start(player, cutsceneId, commandId(wait), endBehavior(wait));
        resume(services.dialogue(), session, wait);
    }

    private void startAndWait(ServicePair services, ServerPlayer player, DialogueSessionView session,
            DialogueExternalWait wait, CutsceneId cutsceneId) {
        services.cutscene().start(player, cutsceneId, commandId(wait), endBehavior(wait));
        waitUntilDone(services, player, session, wait, cutsceneId);
    }

    private void stopAndResume(ServicePair services, ServerPlayer player, DialogueSessionView session,
            DialogueExternalWait wait, CutsceneId cutsceneId) {
        services.cutscene().stop(player, cutsceneId, commandId(wait));
        pending.remove(wait.waitKey());
        resume(services.dialogue(), session, wait);
    }

    private void waitUntilDone(ServicePair services, ServerPlayer player, DialogueSessionView session,
            DialogueExternalWait wait, CutsceneId cutsceneId) {
        if (!services.cutscene().isPlaying(player, cutsceneId)) {
            resume(services.dialogue(), session, wait);
            return;
        }
        waitFor(services, new WaitContext(player, session, wait, cutsceneId),
                PendingCutsceneWaits.PendingKind.DONE);
    }

    private void waitUntilPrepared(ServicePair services, ServerPlayer player, DialogueSessionView session,
            DialogueExternalWait wait, CutsceneId cutsceneId) {
        if (services.cutscene().isPrepared(player, cutsceneId)) {
            resume(services.dialogue(), session, wait);
            return;
        }
        waitFor(services, new WaitContext(player, session, wait, cutsceneId),
                PendingCutsceneWaits.PendingKind.PREPARED);
    }

    private void waitFor(ServicePair services, WaitContext context, PendingCutsceneWaits.PendingKind kind) {
        PendingCutsceneWaits.Pending next = new PendingCutsceneWaits.Pending(context.player().getUUID(),
                context.cutsceneId(), context.session().sessionId(), context.session().revision(),
                context.externalWait().waitKey(), kind);
        next.attach(services.dialogue().observeTerminal(context.session().sessionId(),
                ignored -> stopAfterDialogueTerminal(context.player(), services.cutscene(), next)));
        pending.put(next);
    }

    private void stopAfterDialogueTerminal(
            ServerPlayer player,
            CutsceneService cutscene,
            PendingCutsceneWaits.Pending wait
    ) {
        pending.remove(wait.waitKey()).ifPresent(removed -> cutscene.stop(player, removed.cutsceneId(),
                new CutsceneCommandId("dialogue-terminal:" + removed.waitKey())));
    }

    private void resumeReady(ServerPlayer player, ServicePair services) {
        for (PendingCutsceneWaits.Pending wait : pending.drainReady(player, services.cutscene())) {
            services.dialogue().resumeExternal(new DialogueExternalResumeRequest(wait.sessionId(),
                    wait.revision(), wait.waitKey()));
        }
    }

    private static void resume(DialogueService dialogue, DialogueSessionView session, DialogueExternalWait wait) {
        dialogue.resumeExternal(new DialogueExternalResumeRequest(session.sessionId(), session.revision(),
                wait.waitKey()));
    }

    private static Optional<CutsceneId> cutsceneId(DialogueExternalWait wait) {
        Map<String, String> payload = DialogueCommandPayload.decode(wait.payload());
        return Optional.ofNullable(payload.get("cutscene"))
                .filter(value -> !value.isBlank())
                .map(CutsceneId::parse);
    }

    private static CutsceneEndBehavior endBehavior(DialogueExternalWait wait) {
        Map<String, String> payload = DialogueCommandPayload.decode(wait.payload());
        boolean hold = Boolean.parseBoolean(payload.getOrDefault("holdLastFrame",
                payload.getOrDefault("hold", "false")));
        return hold ? CutsceneEndBehavior.HOLD_LAST_FRAME : CutsceneEndBehavior.AUTO_STOP;
    }

    private static CutsceneCommandId commandId(DialogueExternalWait wait) {
        return new CutsceneCommandId("dialogue:" + wait.waitKey());
    }

    private record ServicePair(DialogueService dialogue, CutsceneService cutscene) {
    }

    private record WaitContext(
            ServerPlayer player,
            DialogueSessionView session,
            DialogueExternalWait externalWait,
            CutsceneId cutsceneId
    ) {
    }
}
