package io.github.kxyr0.echostoryengine.story.forge.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadStatus;
import io.github.kxyr0.echostoryengine.story.forge.EchoStoryEngineMod;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CatalogInstallResult;
import io.github.kxyr0.echostoryengine.story.forge.host.ForgeServerHost;
import io.github.kxyr0.echostoryengine.story.forge.host.ForgeServerHostState;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoStoryEngineMod.MOD_ID)
public final class ForgeHostGameTests {
    static final StoryId STORY_ID = StoryId.of("sample:gametest_story");
    static final ExecutionId EXECUTION_ID = ExecutionId.of("sample:gametest_execution");
    static final String DURABLE_SAVE_TEST = "ForgeHostGameTests.hostExecutesStoryThroughDurableSave";
    static final String RECOVERY_TEST = "ForgeHostGameTests.hostRecreationRecoversWaitingAction";
    static final String INVALID_RELOAD_TEST = "ForgeHostGameTests.invalidCandidateRetainsCatalog";
    static final String COMMAND_TREE_TEST = "ForgeHostGameTests.echoStoryCommandsExecuteOnServerDispatcher";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgeHostGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void hostExecutesStoryThroughDurableSave(GameTestHelper helper) {
        mark(DURABLE_SAVE_TEST);
        ForgeHostGameTestRuntime.TestContext context = ForgeHostGameTestRuntime.createContext(
                helper.getLevel().getServer(),
                "durable-save");
        AtomicInteger dispatches = new AtomicInteger();
        AtomicReference<ForgeServerHost> hostRef = new AtomicReference<>();
        ForgeServerHost host = context.host((state, command) -> {
            dispatches.incrementAndGet();
            ForgeHostGameTestRuntime.assertWaitingSaved(context.repository(), command);
            hostRef.get().completeAction(
                    state.storyId(),
                    state.executionId(),
                    command.actionExecutionId(),
                    ExternalActionResultCategory.SUCCESS);
        });
        hostRef.set(host);
        host.start();
        helper.assertTrue(host.health().state() == ForgeServerHostState.ACTIVE, "host must be active");
        host.startExecution(STORY_ID, EXECUTION_ID);

        helper.succeedWhen(() -> {
            host.tick();
            ExecutionLoadResult loaded = context.repository().load(EXECUTION_ID);
            helper.assertTrue(loaded.status() == ExecutionLoadStatus.FOUND, "checkpoint must exist");
            helper.assertTrue(loaded.checkpoint().orElseThrow().state().phase() == StoryExecutionPhase.FINISHED,
                    "execution must finish after test action completion");
            helper.assertTrue(dispatches.get() == 1, "external command must dispatch once");
            host.stop();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void hostRecreationRecoversWaitingAction(GameTestHelper helper) {
        mark(RECOVERY_TEST);
        ForgeHostGameTestRuntime.TestContext context = ForgeHostGameTestRuntime.createContext(
                helper.getLevel().getServer(),
                "recovery");
        AtomicReference<ActionExecutionId> actionId = new AtomicReference<>();
        AtomicReference<ForgeServerHost> hostRef = new AtomicReference<>();
        AtomicBoolean recreated = new AtomicBoolean();
        ForgeServerHost first = context.host((state, command) -> actionId.set(command.actionExecutionId()));
        hostRef.set(first);
        first.start();
        first.startExecution(STORY_ID, EXECUTION_ID);

        helper.succeedWhen(() -> {
            hostRef.get().tick();
            if (!recreated.get()) {
                ForgeHostGameTestRuntime.waitForWaitingCheckpoint(helper, context.repository(), actionId.get());
                hostRef.get().stop();
                ForgeServerHost second = context.host(ForgeHostGameTestRuntime.recoveryOnlySink(actionId.get()));
                second.start();
                hostRef.set(second);
                second.completeAction(STORY_ID, EXECUTION_ID, actionId.get(), ExternalActionResultCategory.SUCCESS);
                recreated.set(true);
                helper.assertTrue(false, "waiting for recovered completion save");
            }
            ExecutionLoadResult loaded = context.repository().load(EXECUTION_ID);
            helper.assertTrue(loaded.status() == ExecutionLoadStatus.FOUND, "recovered checkpoint must exist");
            helper.assertTrue(loaded.checkpoint().orElseThrow().state().phase() == StoryExecutionPhase.FINISHED,
                    "recovered execution must finish");
            hostRef.get().stop();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void echoStoryCommandsExecuteOnServerDispatcher(GameTestHelper helper) {
        mark(COMMAND_TREE_TEST);
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer()
                .getCommands()
                .getDispatcher();
        var root = dispatcher.getRoot().getChild("echo_story");
        helper.assertTrue(root != null, "echo_story command root must be registered");
        Set<String> children = root.getChildren().stream().map(node -> node.getName()).collect(Collectors.toSet());
        helper.assertTrue(children.equals(Set.of("health", "stories", "executions", "execution", "extensions",
                "extension", "diagnostics")),
                "echo_story command literals must match M4 contract");
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack();

        execute(helper, dispatcher, source, "echo_story health");
        execute(helper, dispatcher, source, "echo_story stories");
        execute(helper, dispatcher, source, "echo_story executions");
        execute(helper, dispatcher, source, "echo_story execution sample:missing");
        execute(helper, dispatcher, source, "echo_story extensions");
        execute(helper, dispatcher, source, "echo_story extension sample:missing");
        execute(helper, dispatcher, source, "echo_story diagnostics");
        execute(helper, dispatcher, source, "echo_story diagnostics 20");
        execute(helper, dispatcher, source, "echo_story diagnostics 0");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void invalidCandidateRetainsCatalog(GameTestHelper helper) {
        mark(INVALID_RELOAD_TEST);
        ForgeHostGameTestRuntime.CatalogContext context = ForgeHostGameTestRuntime.catalogContext();
        StoryResourceCandidate invalid = ForgeHostGameTestRuntime.invalidCandidate(context);

        CatalogInstallResult result = context.catalogState().install(invalid, context.clock().now());

        helper.assertTrue(!result.installed(), "invalid candidate must not install");
        helper.assertTrue(context.catalogState().current().storyCount() == 1,
                "previous catalog must remain visible");
        helper.succeed();
    }

    private static void execute(
            GameTestHelper helper,
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source,
            String command) {
        try {
            int result = dispatcher.execute(command, source);
            helper.assertTrue(result > 0, "command must produce output: " + command);
        } catch (CommandSyntaxException exception) {
            helper.fail("command failed: " + command + " -> " + exception.getMessage());
        }
    }

    private static void mark(String testName) {
        LOGGER.info("ESE-GAMETEST-RUN {}", testName);
    }
}
