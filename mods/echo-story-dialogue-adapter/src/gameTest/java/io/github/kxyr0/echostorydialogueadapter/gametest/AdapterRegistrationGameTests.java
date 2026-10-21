package io.github.kxyr0.echostorydialogueadapter.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueQueryResult;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echostorydialogueadapter.forge.EchoStoryDialogueAdapterMod;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationStatus;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoStoryDialogueAdapterMod.MOD_ID)
public final class AdapterRegistrationGameTests {
    static final String REGISTRATION_TEST =
            "AdapterRegistrationGameTests.storyAndDialogueModsAcceptAdapterExtension";
    static final String TERMINAL_CANCEL_TEST =
            "AdapterRegistrationGameTests.dialogueActionCompletesAfterPublicTerminalCancellation";
    static final String TERMINAL_RECOVERY_TEST =
            "AdapterRegistrationGameTests.recoveredTerminalDialogueActionSubmitsCompletion";
    private static final Logger LOGGER = LogUtils.getLogger();

    private AdapterRegistrationGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void storyAndDialogueModsAcceptAdapterExtension(GameTestHelper helper) {
        mark(REGISTRATION_TEST);
        helper.assertTrue(ModList.get().isLoaded("echo_story_engine"), "Story mod must be loaded");
        helper.assertTrue(ModList.get().isLoaded("echo_dialogue"), "Dialogue mod must be loaded");
        helper.assertTrue(ModList.get().isLoaded(EchoStoryDialogueAdapterMod.MOD_ID), "Adapter mod must be loaded");

        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer()
                .getCommands()
                .getDispatcher();
        AdapterGameTestSupport.assertRootChildren(helper, dispatcher, "echo_story", Set.of("health", "stories",
                "executions",
                "execution", "extensions", "extension", "diagnostics"));
        AdapterGameTestSupport.assertRootChildren(helper, dispatcher, "echo_dialogue", Set.of("health", "dialogues",
                "play",
                "active", "cancel", "diagnostics"));
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void dialogueActionCompletesAfterPublicTerminalCancellation(GameTestHelper helper) {
        mark(TERMINAL_CANCEL_TEST);
        MinecraftServer server = helper.getLevel().getServer();
        ActionExecutionId actionId = ActionExecutionId.of("sample:adapter_cancel_action");
        DialogueSessionId sessionId = AdapterGameTestSupport.sessionId(actionId);
        AtomicReference<ActionCompletion> completion = new AtomicReference<>();
        AtomicReference<StoryExtensionSession> sessionRef = new AtomicReference<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();

        StoryExtensionSession session = AdapterGameTestSupport.extensionSession(server);
        sessionRef.set(session);
        StoryActionHandler handler = session.handlers().get(0);
        var result = handler.start(AdapterGameTestSupport.startRequest(actionId), value -> {
            completion.set(value);
            return ActionCompletionAcceptance.ACCEPTED;
        });
        helper.assertTrue(result.status() == ActionInvocationStatus.ACCEPTED,
                "adapter start must be accepted");
        DialogueService service = AdapterGameTestSupport.dialogueService(server);
        helper.succeedWhen(() -> {
            DialogueQueryResult query = service.query(sessionId);
            helper.assertTrue(query.status() != DialogueOperationStatus.NOT_FOUND,
                    "dialogue session must exist");
            query.session().ifPresent(view -> {
                if (view.status() == DialogueSessionStatus.ACTIVE && !cancelled.get()) {
                    var cancel = service.cancel(sessionId);
                    helper.assertTrue(cancel.status() == DialogueOperationStatus.ACCEPTED,
                            "public cancel must be accepted");
                    cancelled.set(true);
                }
            });
            AdapterGameTestSupport.assertCompletion(helper, completion.get(), sessionId, actionId);
            AdapterGameTestSupport.closeSession(sessionRef, closed);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void recoveredTerminalDialogueActionSubmitsCompletion(GameTestHelper helper) {
        mark(TERMINAL_RECOVERY_TEST);
        MinecraftServer server = helper.getLevel().getServer();
        ActionExecutionId actionId = AdapterGameTestSupport.uniqueActionId("adapter_recovered_terminal_action");
        DialogueSessionId sessionId = AdapterGameTestSupport.sessionId(actionId);
        DialogueService service = AdapterGameTestSupport.dialogueService(server);
        AtomicReference<ActionCompletion> unexpectedStartCompletion = new AtomicReference<>();
        AdapterGameTestSupport.RecoveryCapture recovered = new AdapterGameTestSupport.RecoveryCapture();
        AtomicBoolean firstClosed = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean recoveredClosed = new AtomicBoolean();

        StoryExtensionSession firstSession = AdapterGameTestSupport.extensionSession(server);
        var start = firstSession.handlers().get(0).start(AdapterGameTestSupport.startRequest(actionId), value -> {
            unexpectedStartCompletion.set(value);
            return ActionCompletionAcceptance.ACCEPTED;
        });
        helper.assertTrue(start.status() == ActionInvocationStatus.ACCEPTED,
                "initial adapter start must be accepted");
        helper.succeedWhen(() -> {
            DialogueQueryResult active = service.query(sessionId);
            helper.assertTrue(active.status() != DialogueOperationStatus.NOT_FOUND,
                    "dialogue session must exist before recovery");
            if (!firstClosed.get()) {
                helper.assertTrue(active.session().orElseThrow().status() == DialogueSessionStatus.ACTIVE,
                        "dialogue must be ACTIVE before closing initial observer");
                firstSession.close();
                firstClosed.set(true);
            }
            AdapterGameTestSupport.cancelWhenInstalled(helper, service, sessionId, cancelled);
            AdapterGameTestSupport.awaitTerminal(helper, service, sessionId);
            helper.assertTrue(unexpectedStartCompletion.get() == null,
                    "closed initial observer must not receive terminal completion");
            recovered.recover(helper, server, actionId);
            AdapterGameTestSupport.assertCompletion(helper, recovered.completion(), sessionId, actionId);
            recovered.close(recoveredClosed);
        });
    }

    private static void mark(String testName) {
        LOGGER.info("ESDA-GAMETEST-RUN {}", testName);
    }
}
