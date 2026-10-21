package io.github.kxyr0.echodialoguecutscene.forge.screen;

import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import io.github.kxyr0.echodialogue.api.command.DialogueCommandPayload;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echodialoguecutscene.forge.wait.DialogueCutsceneServices;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public final class ScreenCommandScheduler {
    private final DialogueCutsceneServices services;
    private final Logger logger;
    private final Map<String, PendingDialogue> dialogueWaits = new HashMap<>();
    private final Map<ActionExecutionId, PendingStory> storyWaits = new HashMap<>();

    public ScreenCommandScheduler(DialogueCutsceneServices services, Logger logger) {
        this.services = services;
        this.logger = logger;
    }

    @SubscribeEvent
    public void onDialogueExternalWait(DialogueExternalWaitDispatchEvent event) {
        if (!ScreenStoryIds.WAIT_TYPE.equals(event.externalWait().waitType()) || event.player().isEmpty()) {
            return;
        }
        handleDialogueWait(event.player().orElseThrow(), event.session(), event.externalWait());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        long now = Util.getMillis();
        drainDialogue(event.getServer(), now);
        drainStory(now);
    }

    public ActionInvocationResult startStory(
            MinecraftServer server,
            StoryContext context,
            Map<String, String> payload,
            ActionCompletionSink sink
    ) {
        if (!ScreenStoryIds.SET_BLACK.equals(context.actionType())) {
            return ActionInvocationResult.unsupported("Unsupported action type: " + context.actionType());
        }
        Optional<ScreenPayload> decoded = decode(payload, "story action " + context.actionId().asString());
        if (decoded.isEmpty()) {
            return ActionInvocationResult.rejected("Invalid echo_screen:set_black payload.");
        }
        if (!applyServer(server, decoded.orElseThrow())) {
            return ActionInvocationResult.rejected("Echo Cutscene service is unavailable.");
        }
        scheduleOrComplete(context, decoded.orElseThrow(), sink);
        return ActionInvocationResult.accepted();
    }

    public ActionInvocationResult cancelStory(ActionCancelRequest request) {
        if (!ScreenStoryIds.SET_BLACK.equals(request.actionType())) {
            return ActionInvocationResult.unsupported("Unsupported action type: " + request.actionType());
        }
        storyWaits.remove(request.actionExecutionId());
        return ActionInvocationResult.accepted();
    }

    private void handleDialogueWait(ServerPlayer player, DialogueSessionView session, DialogueExternalWait wait) {
        Optional<ScreenPayload> payload = decode(DialogueCommandPayload.decode(wait.payload()), wait.waitKey());
        MinecraftServer server = player.getServer();
        if (server == null || payload.isEmpty() || !applyPlayer(player, payload.orElseThrow())) {
            resumeDialogue(server, session.sessionId(), session.revision(), wait.waitKey());
            return;
        }
        if (payload.orElseThrow().timeMillis() <= 0L) {
            resumeDialogue(server, session.sessionId(), session.revision(), wait.waitKey());
            return;
        }
        dialogueWaits.put(wait.waitKey(), new PendingDialogue(session.sessionId(), session.revision(),
                wait.waitKey(), dueMillis(payload.orElseThrow())));
    }

    private Optional<ScreenPayload> decode(Map<String, String> payload, String source) {
        try {
            return Optional.of(ScreenPayload.decode(payload));
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid echo_screen:set_black payload for {}.", source, exception);
            return Optional.empty();
        }
    }

    private boolean applyPlayer(ServerPlayer player, ScreenPayload payload) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        Optional<CutsceneService> service = services.cutscene(server);
        service.ifPresent(value -> value.setBlack(player, payload.canvasGroup(), payload.timeMillis()));
        return service.isPresent();
    }

    private boolean applyServer(MinecraftServer server, ScreenPayload payload) {
        Optional<CutsceneService> service = services.cutscene(server);
        service.ifPresent(value -> server.getPlayerList().getPlayers()
                .forEach(player -> value.setBlack(player, payload.canvasGroup(), payload.timeMillis())));
        return service.isPresent();
    }

    private void scheduleOrComplete(StoryContext context, ScreenPayload payload, ActionCompletionSink sink) {
        if (payload.timeMillis() <= 0L) {
            completeStory(context, payload, sink);
            return;
        }
        storyWaits.put(context.actionId(), new PendingStory(context, payload, sink, dueMillis(payload)));
    }

    private void drainDialogue(MinecraftServer server, long now) {
        services.dialogue(server).ifPresent(dialogue -> drainDialogue(dialogue, now));
    }

    private void drainDialogue(DialogueService dialogue, long now) {
        Iterator<PendingDialogue> iterator = dialogueWaits.values().iterator();
        while (iterator.hasNext()) {
            PendingDialogue wait = iterator.next();
            if (now >= wait.dueMillis()) {
                iterator.remove();
                resumeDialogue(dialogue, wait);
            }
        }
    }

    private void drainStory(long now) {
        Iterator<PendingStory> iterator = storyWaits.values().iterator();
        while (iterator.hasNext()) {
            PendingStory wait = iterator.next();
            if (now >= wait.dueMillis()) {
                iterator.remove();
                completeStory(wait.context(), wait.payload(), wait.sink());
            }
        }
    }

    private void resumeDialogue(MinecraftServer server, DialogueSessionId sessionId, int revision, String waitKey) {
        if (server != null) {
            services.dialogue(server).ifPresent(dialogue -> resumeDialogue(dialogue, sessionId, revision, waitKey));
        }
    }

    private void resumeDialogue(DialogueService dialogue, PendingDialogue wait) {
        resumeDialogue(dialogue, wait.sessionId(), wait.revision(), wait.waitKey());
    }

    private void resumeDialogue(DialogueService dialogue, DialogueSessionId sessionId, int revision, String waitKey) {
        dialogue.resumeExternal(new DialogueExternalResumeRequest(sessionId, revision, waitKey));
    }

    private void completeStory(StoryContext context, ScreenPayload payload, ActionCompletionSink sink) {
        sink.complete(new ActionCompletion(context.storyId(), context.executionId(), context.actionId(),
                context.actionType(), ExternalActionResultCategory.SUCCESS, payload.outputs()));
    }

    private static long dueMillis(ScreenPayload payload) {
        return Util.getMillis() + payload.timeMillis();
    }
    public record StoryContext(
            StoryId storyId,
            ExecutionId executionId,
            ActionExecutionId actionId,
            NamespacedId actionType
    ) {
        static StoryContext from(ActionStartRequest request) {
            return new StoryContext(request.storyId(), request.executionId(), request.actionExecutionId(),
                    request.actionType());
        }

        static StoryContext from(ActionRecoveryRequest request) {
            return new StoryContext(request.storyId(), request.executionId(), request.actionExecutionId(),
                    request.actionType());
        }
    }
    private record PendingDialogue(
            DialogueSessionId sessionId,
            int revision,
            String waitKey,
            long dueMillis
    ) {
    }
    private record PendingStory(
            StoryContext context,
            ScreenPayload payload,
            ActionCompletionSink sink,
            long dueMillis
    ) {
    }
}
