package io.github.kxyr0.echostorydialogueadapter.gametest;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueQueryResult;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echostorydialogueadapter.bridge.DialogueBridgeExtensionFactory;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationStatus;
import io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

final class AdapterGameTestSupport {
    private AdapterGameTestSupport() {
    }

    static void assertRootChildren(
            GameTestHelper helper,
            CommandDispatcher<CommandSourceStack> dispatcher,
            String rootName,
            Set<String> expected) {
        var root = dispatcher.getRoot().getChild(rootName);
        helper.assertTrue(root != null, rootName + " command root must be registered");
        Set<String> children = root.getChildren().stream().map(node -> node.getName()).collect(Collectors.toSet());
        helper.assertTrue(children.equals(expected), rootName + " command children must match public surface");
    }

    static ActionStartRequest startRequest(ActionExecutionId actionId) {
        return new ActionStartRequest(StoryId.of("sample:adapter_story"), ExecutionId.of("sample:adapter_execution"),
                actionId, NamespacedId.of("echo_dialogue:play"),
                Map.of("dialogueId", "sample:greeting", "audienceId", "adapter-offline-audience"));
    }

    static StoryExtensionSession extensionSession(MinecraftServer server) {
        return new DialogueBridgeExtensionFactory()
                .create(new ForgeStoryExtensionContext(server, StoryExtensionApiVersion.current()));
    }

    static ActionExecutionId uniqueActionId(String prefix) {
        return ActionExecutionId.of("sample:" + prefix + "_" + Long.toHexString(System.nanoTime()));
    }

    static DialogueSessionId sessionId(ActionExecutionId actionId) {
        return new DialogueSessionId("story-action/" + actionId.asString());
    }

    static void cancelWhenInstalled(
            GameTestHelper helper,
            DialogueService service,
            DialogueSessionId sessionId,
            AtomicBoolean cancelled) {
        if (cancelled.get()) {
            return;
        }
        var cancel = service.cancel(sessionId);
        helper.assertTrue(cancel.status() == DialogueOperationStatus.ACCEPTED
                        || cancel.status() == DialogueOperationStatus.NOT_FOUND,
                "public cancel must wait for installation or be accepted");
        if (cancel.status() != DialogueOperationStatus.ACCEPTED) {
            helper.assertTrue(false, "waiting for active dialogue installation");
        }
        cancelled.set(true);
    }

    static void awaitTerminal(GameTestHelper helper, DialogueService service, DialogueSessionId sessionId) {
        DialogueQueryResult terminal = service.query(sessionId);
        if (terminal.status() != DialogueOperationStatus.ALREADY_TERMINAL) {
            helper.assertTrue(false, "waiting for durable terminal view");
        }
        helper.assertTrue(terminal.session().orElseThrow().status() == DialogueSessionStatus.CANCELLED,
                "terminal view must be CANCELLED");
    }

    static DialogueService dialogueService(MinecraftServer server) {
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().size() != 1) {
            throw new AssertionError("expected exactly one public Dialogue service provider");
        }
        return event.providers().get(0);
    }

    static void assertCompletion(
            GameTestHelper helper,
            ActionCompletion completion,
            DialogueSessionId sessionId,
            ActionExecutionId actionId) {
        helper.assertTrue(completion != null, "terminal observer must submit Story completion");
        helper.assertTrue(completion.actionExecutionId().equals(actionId), "completion must keep ActionExecutionId");
        helper.assertTrue(completion.resultCategory() == ExternalActionResultCategory.CANCELLATION,
                "cancelled dialogue must map to Story cancellation");
        helper.assertTrue("CANCELLED".equals(completion.outputs().get("dialogue.status")),
                "completion must expose CANCELLED status");
        helper.assertTrue(sessionId.toString().equals(completion.outputs().get("dialogue.sessionId")),
                "completion must use deterministic DialogueSessionId");
    }

    static void closeSession(AtomicReference<StoryExtensionSession> sessionRef, AtomicBoolean closed) {
        StoryExtensionSession session = sessionRef.get();
        if (session != null && closed.compareAndSet(false, true)) {
            session.close();
        }
    }

    static final class RecoveryCapture {
        private final AtomicReference<ActionCompletion> completion = new AtomicReference<>();
        private final AtomicReference<StoryExtensionSession> sessionRef = new AtomicReference<>();
        private final AtomicBoolean recovered = new AtomicBoolean();

        ActionCompletion completion() {
            return completion.get();
        }

        void recover(GameTestHelper helper, MinecraftServer server, ActionExecutionId actionId) {
            if (recovered.get()) {
                return;
            }
            StoryExtensionSession session = extensionSession(server);
            sessionRef.set(session);
            var result = session.handlers().get(0).recover(recoveryRequest(actionId), value -> {
                completion.set(value);
                return ActionCompletionAcceptance.ACCEPTED;
            });
            helper.assertTrue(result.status() == ActionInvocationStatus.ACCEPTED,
                    "terminal adapter recovery must be accepted");
            recovered.set(true);
        }

        void close(AtomicBoolean closed) {
            closeSession(sessionRef, closed);
        }
    }

    private static ActionRecoveryRequest recoveryRequest(ActionExecutionId actionId) {
        return new ActionRecoveryRequest(StoryId.of("sample:adapter_story"),
                ExecutionId.of("sample:adapter_execution"), actionId, NamespacedId.of("echo_dialogue:play"),
                Map.of("dialogueId", "sample:greeting", "audienceId", "adapter-offline-audience"));
    }
}
