package io.github.kxyr0.echostorycontentpack.acceptance;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueQueryResult;
import io.github.kxyr0.echodialogue.api.service.DialogueService;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echodialogue.forge.network.event.ServerboundDialogueAdvanceEvent;
import io.github.kxyr0.echodialogue.forge.network.event.ServerboundDialogueChoiceEvent;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueAdvanceMessage;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueSelectChoiceMessage;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupResult;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

@Mod(EchoStoryContentPackAcceptanceMod.MOD_ID)
public final class EchoStoryContentPackAcceptanceMod {
    public static final String MOD_ID = "echo_story_content_pack_acceptance";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ACCEPTANCE_AUDIENCE_UUID = "00000000-0000-0000-0000-0000000000c1";
    private static final GameProfile ACCEPTANCE_PROFILE =
            new GameProfile(UUID.fromString(ACCEPTANCE_AUDIENCE_UUID), "C1Acceptance");
    private static final ActorId ACTOR_ID = new ActorId("jdh:content_pack_father");
    private static final ResourceLocation ECHO_NPC_ENTITY_TYPE = new ResourceLocation("echo_npc", "echo_npc");
    private static final int TIMEOUT_TICKS = 800;
    private static final int AUTO_START_TIMEOUT_TICKS = 200;
    private static final int ACTION_INTERVAL_TICKS = 10;
    private static final String AUTO_MODE = System.getProperty("echoStoryContentPackAcceptance.autoMode", "");
    private static final Map<String, Scenario> SCENARIOS = Map.of(
            "accepted", new Scenario(
                    "accepted",
                    "jdh:content_pack_smoke",
                    "accept",
                    "jdh:content_pack_accepted"),
            "refused", new Scenario(
                    "refused",
                    "jdh:content_pack_smoke_refused",
                    "refuse",
                    "jdh:content_pack_refused"));

    private ActiveRun activeRun;
    private MinecraftServer pendingAutoStartServer;
    private int pendingAutoStartTicks;
    private int autoScenarioIndex;
    private int autoPasses;

    public EchoStoryContentPackAcceptanceMod() {
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_story_content_pack_acceptance")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("run")
                        .then(Commands.argument("scenario", StringArgumentType.word())
                                .executes(context -> run(context, defaultExecutionId(context)))
                                .then(Commands.argument("executionId", StringArgumentType.greedyString())
                                        .executes(context -> run(context,
                                                StringArgumentType.getString(context, "executionId"))))))
                .then(Commands.literal("start_only")
                        .then(Commands.argument("scenario", StringArgumentType.word())
                                .then(Commands.argument("executionId", StringArgumentType.greedyString())
                                        .executes(context -> startOnly(context,
                                                StringArgumentType.getString(context, "executionId"))))))
                .then(Commands.literal("run_until_choice")
                        .then(Commands.argument("scenario", StringArgumentType.word())
                                .then(Commands.argument("executionId", StringArgumentType.greedyString())
                                        .executes(context -> runUntilChoice(context,
                                                StringArgumentType.getString(context, "executionId"))))))
                .then(Commands.literal("resume")
                        .then(Commands.argument("scenario", StringArgumentType.word())
                                .then(Commands.argument("executionId", StringArgumentType.greedyString())
                                        .executes(context -> resume(context,
                                                StringArgumentType.getString(context, "executionId"))))))
                .then(Commands.literal("resume_choice")
                        .then(Commands.argument("scenario", StringArgumentType.word())
                                .then(Commands.argument("executionId", StringArgumentType.greedyString())
                                        .executes(context -> resumeChoice(context,
                                                StringArgumentType.getString(context, "executionId"))))))
                .then(Commands.literal("cleanup").executes(this::cleanup))
                .then(Commands.literal("status").executes(this::status)));
    }

    private int run(CommandContext<CommandSourceStack> context, String rawExecutionId) {
        return startManaged(context, rawExecutionId, false);
    }

    private int runUntilChoice(CommandContext<CommandSourceStack> context, String rawExecutionId) {
        return startManaged(context, rawExecutionId, true);
    }

    private int startOnly(CommandContext<CommandSourceStack> context, String rawExecutionId) {
        Optional<Scenario> scenario = scenario(context);
        if (scenario.isEmpty()) {
            return 0;
        }
        if (activeRun != null) {
            sendFailure(context, "Acceptance уже выполняется: " + activeRun.summary());
            return 0;
        }
        MinecraftServer server = context.getSource().getServer();
        try {
            ExecutionId executionId = ExecutionId.of(rawExecutionId);
            EchoStoryExecutionLookupResult lookup = EchoStoryExecutionBridge.lookup(server);
            if (lookup.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) {
                sendFailure(context, lookup.message());
                return 0;
            }
            lookup.starter().orElseThrow().start(StoryId.of(scenario.orElseThrow().storyId()), executionId);
            LOGGER.info("ESCPC-ACCEPTANCE START_ONLY scenario={} storyId={} executionId={} sessionId={}",
                    scenario.orElseThrow().key, scenario.orElseThrow().storyId, executionId.asString(),
                    dialogueSessionId(executionId.asString()));
            sendSuccess(context, "Acceptance story start_only выполнен: scenario=" + scenario.orElseThrow().key
                    + ", executionId=" + executionId.asString());
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            sendFailure(context, "Acceptance start_only не выполнен: " + exception.getMessage());
            return 0;
        }
    }

    private int startManaged(CommandContext<CommandSourceStack> context, String rawExecutionId, boolean pauseAtChoice) {
        Optional<Scenario> scenario = scenario(context);
        if (scenario.isEmpty()) {
            return 0;
        }
        if (activeRun != null) {
            sendFailure(context, "Acceptance уже выполняется: " + activeRun.summary());
            return 0;
        }
        MinecraftServer server = context.getSource().getServer();
        try {
            ExecutionId executionId = ExecutionId.of(rawExecutionId);
            EchoStoryExecutionLookupResult lookup = EchoStoryExecutionBridge.lookup(server);
            if (lookup.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) {
                sendFailure(context, lookup.message());
                return 0;
            }
            lookup.starter().orElseThrow().start(StoryId.of(scenario.orElseThrow().storyId()), executionId);
            activeRun = ActiveRun.started(server, scenario.orElseThrow(), executionId.asString(), pauseAtChoice);
            LOGGER.info("ESCPC-ACCEPTANCE START scenario={} storyId={} executionId={} sessionId={} pauseAtChoice={}",
                    activeRun.scenario.key, activeRun.scenario.storyId, activeRun.executionId,
                    activeRun.dialogueSessionId, pauseAtChoice);
            sendSuccess(context, "Acceptance запущен: " + activeRun.summary());
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            sendFailure(context, "Acceptance run не стартовал: " + exception.getMessage());
            return 0;
        }
    }

    private int resume(CommandContext<CommandSourceStack> context, String rawExecutionId) {
        Optional<Scenario> scenario = scenario(context);
        if (scenario.isEmpty()) {
            return 0;
        }
        if (activeRun != null) {
            sendFailure(context, "Acceptance уже выполняется: " + activeRun.summary());
            return 0;
        }
        try {
            ExecutionId executionId = ExecutionId.of(rawExecutionId);
            activeRun = ActiveRun.resumed(context.getSource().getServer(), scenario.orElseThrow(),
                    executionId.asString());
            LOGGER.info("ESCPC-ACCEPTANCE RESUME scenario={} storyId={} executionId={} sessionId={}",
                    activeRun.scenario.key, activeRun.scenario.storyId, activeRun.executionId,
                    activeRun.dialogueSessionId);
            sendSuccess(context, "Acceptance resume активирован: " + activeRun.summary());
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            sendFailure(context, "Acceptance resume не стартовал: " + exception.getMessage());
            return 0;
        }
    }

    private int resumeChoice(CommandContext<CommandSourceStack> context, String rawExecutionId) {
        Optional<Scenario> scenario = scenario(context);
        if (scenario.isEmpty()) {
            return 0;
        }
        if (activeRun != null) {
            sendFailure(context, "Acceptance уже выполняется: " + activeRun.summary());
            return 0;
        }
        try {
            ExecutionId executionId = ExecutionId.of(rawExecutionId);
            activeRun = ActiveRun.resumedAtChoice(context.getSource().getServer(), scenario.orElseThrow(),
                    executionId.asString());
            LOGGER.info("ESCPC-ACCEPTANCE RESUME_CHOICE scenario={} storyId={} executionId={} sessionId={}",
                    activeRun.scenario.key, activeRun.scenario.storyId, activeRun.executionId,
                    activeRun.dialogueSessionId);
            sendSuccess(context, "Acceptance resume_choice активирован: " + activeRun.summary());
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            sendFailure(context, "Acceptance resume_choice не стартовал: " + exception.getMessage());
            return 0;
        }
    }

    private int cleanup(CommandContext<CommandSourceStack> context) {
        activeRun = null;
        Optional<NpcService> service = npcService(context.getSource().getServer());
        if (service.isEmpty()) {
            sendFailure(context, "Echo NPC service недоступен.");
            return 0;
        }
        NpcResults.Despawn result = service.orElseThrow().despawn(ACTOR_ID);
        LOGGER.info("ESCPC-ACCEPTANCE CLEANUP actorId={} status={} message={}",
                ACTOR_ID.value(), result.status(), result.message());
        sendSuccess(context, "Cleanup actor=" + ACTOR_ID.value() + " status=" + result.status());
        return Command.SINGLE_SUCCESS;
    }

    private int status(CommandContext<CommandSourceStack> context) {
        if (activeRun == null) {
            sendSuccess(context, "Acceptance idle.");
            return Command.SINGLE_SUCCESS;
        }
        sendSuccess(context, activeRun.summary());
        return Command.SINGLE_SUCCESS;
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (pendingAutoStartServer != null && activeRun == null) {
            tryStartPendingAuto();
        }
        if (activeRun == null) {
            return;
        }
        try {
            tick(activeRun);
        } catch (RuntimeException exception) {
            fail(activeRun, "exception=" + exception.getClass().getSimpleName() + ":" + exception.getMessage());
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        if ("productionSmoke".equals(AUTO_MODE)) {
            productionSmoke(event.getServer());
        } else if ("acceptance".equals(AUTO_MODE)) {
            pendingAutoStartServer = event.getServer();
            pendingAutoStartTicks = 0;
        }
    }

    private void productionSmoke(MinecraftServer server) {
        LOGGER.info("ESCPC-ACCEPTANCE PRODUCTION_SMOKE_START");
        if (!allRequiredModsLoaded()) {
            LOGGER.error("ESCPC-ACCEPTANCE PRODUCTION_SMOKE_FAIL reason=missing-required-mod");
        } else if (ModList.get().isLoaded("echo_animation_authoring")) {
            LOGGER.error("ESCPC-ACCEPTANCE PRODUCTION_SMOKE_FAIL reason=authoring-mod-present");
        } else {
            LOGGER.info("ESCPC-ACCEPTANCE PRODUCTION_SMOKE_PASS mods=echo_animation,echo_npc,echo_dialogue,"
                    + "echo_story_dialogue_adapter,echo_story_engine,echo_story_npc_adapter,echo_story_content_pack");
        }
        server.executeIfPossible(server::stopServer);
    }

    private void tick(ActiveRun run) {
        run.ticks++;
        if (run.ticks > TIMEOUT_TICKS) {
            fail(run, "timeout step=" + run.step);
            return;
        }
        switch (run.step) {
            case WAIT_NPC -> waitNpc(run);
            case WAIT_DIALOGUE_REVISION_0 -> waitDialogueRevision(run, 0, Step.ADVANCE_TO_SECOND_LINE);
            case ADVANCE_TO_SECOND_LINE -> postAdvance(run, 0, Step.WAIT_DIALOGUE_REVISION_1);
            case WAIT_DIALOGUE_REVISION_1 -> waitDialogueRevision(run, 1, Step.ADVANCE_TO_CHOICE);
            case ADVANCE_TO_CHOICE -> postAdvance(run, 1, Step.WAIT_DIALOGUE_REVISION_2);
            case WAIT_DIALOGUE_REVISION_2 -> waitDialogueRevision(run, 2, Step.SELECT_CHOICE);
            case SELECT_CHOICE -> postChoice(run, 2, Step.WAIT_DIALOGUE_REVISION_3);
            case WAIT_DIALOGUE_REVISION_3 -> waitDialogueRevision(run, 3, Step.ADVANCE_BRANCH_LINE);
            case ADVANCE_BRANCH_LINE -> postAdvance(run, 3, Step.WAIT_DIALOGUE_TERMINAL);
            case WAIT_DIALOGUE_TERMINAL -> waitTerminal(run);
            case WAIT_STORY_SETTLE -> waitStorySettle(run);
        }
    }

    private void waitNpc(ActiveRun run) {
        if (!onInterval(run)) {
            return;
        }
        Optional<NpcService> service = npcService(run.server);
        if (service.isEmpty()) {
            return;
        }
        NpcResults.Query query = service.orElseThrow().query(ACTOR_ID);
        if (query.status() != NpcResults.QueryStatus.FOUND || query.actor().isEmpty()) {
            return;
        }
        NpcActorView actor = query.actor().orElseThrow();
        if (!materialized(actor)) {
            return;
        }
        int entities = echoNpcEntityCount(run.server.overworld(), actor.position().x(), actor.position().y(),
                actor.position().z());
        LOGGER.info("ESCPC-ACCEPTANCE NPC_MATERIALIZED scenario={} actorId={} definitionId={} display={} "
                        + "status={} entities={}",
                run.scenario.key, actor.actorId().value(), actor.definitionId().value(),
                actor.displayName(), actor.status(), entities);
        if (entities != 1) {
            fail(run, "npc-entity-count=" + entities);
            return;
        }
        run.step = Step.WAIT_DIALOGUE_REVISION_0;
        run.stepTicks = 0;
    }

    private void waitDialogueRevision(ActiveRun run, int revision, Step next) {
        run.stepTicks++;
        if (!onInterval(run)) {
            return;
        }
        Optional<DialogueSessionView> session = querySession(run);
        if (session.isEmpty()) {
            return;
        }
        DialogueSessionView view = session.orElseThrow();
        if (view.status() != DialogueSessionStatus.ACTIVE || view.revision() < revision) {
            return;
        }
        LOGGER.info("ESCPC-ACCEPTANCE DIALOGUE_ACTIVE scenario={} sessionId={} revision={} expectedRevision={} "
                        + "status={}",
                run.scenario.key, run.dialogueSessionId, view.revision(), revision, view.status());
        if (run.pauseAtChoice && next == Step.SELECT_CHOICE) {
            LOGGER.info("ESCPC-ACCEPTANCE PAUSE_CHOICE scenario={} sessionId={} revision={} executionId={}",
                    run.scenario.key, run.dialogueSessionId, view.revision(), run.executionId);
            activeRun = null;
            return;
        }
        run.step = next;
        run.stepTicks = 0;
    }

    private void postAdvance(ActiveRun run, int expectedRevision, Step next) {
        if (!onInterval(run)) {
            return;
        }
        try {
            MinecraftForge.EVENT_BUS.post(new ServerboundDialogueAdvanceEvent(
                    new DialogueAdvanceMessage(run.dialogueSessionId, expectedRevision), fakePlayer(run.server)));
            LOGGER.info("ESCPC-ACCEPTANCE DIALOGUE_ADVANCE scenario={} sessionId={} expectedRevision={}",
                    run.scenario.key, run.dialogueSessionId, expectedRevision);
            run.step = next;
            run.stepTicks = 0;
        } catch (RuntimeException exception) {
            fail(run, "advance-failed revision=" + expectedRevision + " message=" + exception.getMessage());
        }
    }

    private void postChoice(ActiveRun run, int expectedRevision, Step next) {
        if (!onInterval(run)) {
            return;
        }
        try {
            MinecraftForge.EVENT_BUS.post(new ServerboundDialogueChoiceEvent(
                    new DialogueSelectChoiceMessage(run.dialogueSessionId, expectedRevision,
                            run.scenario.choiceId),
                    fakePlayer(run.server)));
            LOGGER.info("ESCPC-ACCEPTANCE DIALOGUE_CHOICE scenario={} sessionId={} expectedRevision={} choiceId={}",
                    run.scenario.key, run.dialogueSessionId, expectedRevision, run.scenario.choiceId);
            run.step = next;
            run.stepTicks = 0;
        } catch (RuntimeException exception) {
            fail(run, "choice-failed choiceId=" + run.scenario.choiceId + " message=" + exception.getMessage());
        }
    }

    private void waitTerminal(ActiveRun run) {
        run.stepTicks++;
        if (!onInterval(run)) {
            return;
        }
        Optional<DialogueSessionView> session = querySession(run);
        if (session.isEmpty()) {
            return;
        }
        DialogueSessionView view = session.orElseThrow();
        if (view.status() == DialogueSessionStatus.ACTIVE) {
            return;
        }
        String outcome = view.terminalOutcomeId().map(DialogueOutcomeId::toString).orElse("");
        LOGGER.info("ESCPC-ACCEPTANCE DIALOGUE_TERMINAL scenario={} sessionId={} status={} outcome={}",
                run.scenario.key, run.dialogueSessionId, view.status(), outcome);
        if (!run.scenario.expectedOutcome.equals(outcome)) {
            fail(run, "unexpected-outcome expected=" + run.scenario.expectedOutcome + " actual=" + outcome);
            return;
        }
        run.step = Step.WAIT_STORY_SETTLE;
        run.stepTicks = 0;
    }

    private void waitStorySettle(ActiveRun run) {
        run.stepTicks++;
        if (run.stepTicks < 40) {
            return;
        }
        Optional<NpcService> service = npcService(run.server);
        int entities = service.flatMap(value -> value.query(ACTOR_ID).actor())
                .map(actor -> echoNpcEntityCount(run.server.overworld(), actor.position().x(), actor.position().y(),
                        actor.position().z()))
                .orElse(-1);
        if (entities != 1) {
            fail(run, "post-story-npc-entity-count=" + entities);
            return;
        }
        pass(run);
    }

    private void pass(ActiveRun run) {
        LOGGER.info("ESCPC-ACCEPTANCE PASS scenario={} storyId={} executionId={} outcome={} actorId={} entities=1",
                run.scenario.key, run.scenario.storyId, run.executionId, run.scenario.expectedOutcome,
                ACTOR_ID.value());
        activeRun = null;
        if ("acceptance".equals(AUTO_MODE)) {
            autoPasses++;
            startNextAutoScenario(run.server);
        }
    }

    private Optional<DialogueSessionView> querySession(ActiveRun run) {
        Optional<DialogueService> service = dialogueService(run.server);
        if (service.isEmpty()) {
            return Optional.empty();
        }
        DialogueQueryResult query = service.orElseThrow().query(new DialogueSessionId(run.dialogueSessionId));
        if (query.status() == DialogueOperationStatus.NOT_FOUND
                || query.status() == DialogueOperationStatus.SERVICE_UNAVAILABLE) {
            return Optional.empty();
        }
        return query.session();
    }

    private Optional<DialogueService> dialogueService(MinecraftServer server) {
        EchoDialogueServiceLookupEvent event = new EchoDialogueServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.providers().size() != 1) {
            return Optional.empty();
        }
        return Optional.of(event.providers().get(0));
    }

    private Optional<NpcService> npcService(MinecraftServer server) {
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
        if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED) {
            return Optional.empty();
        }
        return lookup.service();
    }

    private ServerPlayer fakePlayer(MinecraftServer server) {
        return FakePlayerFactory.get(server.overworld(), ACCEPTANCE_PROFILE);
    }

    private int echoNpcEntityCount(ServerLevel level, double x, double y, double z) {
        AABB bounds = new AABB(x, y, z, x, y, z).inflate(8.0D);
        return level.getEntities((Entity) null, bounds, this::isEchoNpc).size();
    }

    private boolean isEchoNpc(Entity entity) {
        return ECHO_NPC_ENTITY_TYPE.equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }

    private boolean materialized(NpcActorView actor) {
        return actor.status() == NpcActorView.Status.MATERIALIZED && actor.materializedPosition().isPresent();
    }

    private boolean onInterval(ActiveRun run) {
        run.stepTicks++;
        return run.stepTicks == 1 || run.stepTicks % ACTION_INTERVAL_TICKS == 0;
    }

    private boolean allRequiredModsLoaded() {
        return requiredMods().stream().allMatch(((Predicate<String>) ModList.get()::isLoaded));
    }

    private java.util.List<String> requiredMods() {
        return java.util.List.of("echo_animation", "echo_npc", "echo_dialogue", "echo_story_dialogue_adapter",
                "echo_story_engine", "echo_story_npc_adapter", "echo_story_content_pack");
    }

    private void startNextAutoScenario(MinecraftServer server) {
        java.util.List<Scenario> scenarios = java.util.List.of(SCENARIOS.get("accepted"), SCENARIOS.get("refused"));
        if (autoScenarioIndex >= scenarios.size()) {
            LOGGER.info("ESCPC-ACCEPTANCE AUTO_PASS scenarios=accepted,refused passes={}", autoPasses);
            return;
        }
        Scenario scenario = scenarios.get(autoScenarioIndex++);
        String executionId = "jdh:content_pack_auto_" + scenario.key;
        try {
            EchoStoryExecutionLookupResult lookup = EchoStoryExecutionBridge.lookup(server);
            if (lookup.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) {
                failAuto(server, lookup.message());
                return;
            }
            lookup.starter().orElseThrow().start(StoryId.of(scenario.storyId), ExecutionId.of(executionId));
            activeRun = ActiveRun.started(server, scenario, executionId, false);
            LOGGER.info("ESCPC-ACCEPTANCE AUTO_START scenario={} storyId={} executionId={} sessionId={}",
                    scenario.key, scenario.storyId, executionId, activeRun.dialogueSessionId);
        } catch (RuntimeException exception) {
            failAuto(server, exception.getClass().getSimpleName() + ":" + exception.getMessage());
        }
    }

    private void tryStartPendingAuto() {
        pendingAutoStartTicks++;
        EchoStoryExecutionLookupResult lookup = EchoStoryExecutionBridge.lookup(pendingAutoStartServer);
        if (lookup.status() == EchoStoryExecutionLookupResult.Status.RESOLVED) {
            MinecraftServer server = pendingAutoStartServer;
            pendingAutoStartServer = null;
            pendingAutoStartTicks = 0;
            startNextAutoScenario(server);
            return;
        }
        if (pendingAutoStartTicks > AUTO_START_TIMEOUT_TICKS) {
            MinecraftServer server = pendingAutoStartServer;
            pendingAutoStartServer = null;
            failAuto(server, lookup.message());
        }
    }

    private void failAuto(MinecraftServer server, String reason) {
        LOGGER.error("ESCPC-ACCEPTANCE AUTO_FAIL reason={}", reason);
        activeRun = null;
        pendingAutoStartServer = null;
        server.executeIfPossible(server::stopServer);
    }

    private Optional<Scenario> scenario(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "scenario").toLowerCase(Locale.ROOT);
        Scenario scenario = SCENARIOS.get(key);
        if (scenario == null) {
            sendFailure(context, "Неизвестный scenario: " + key + ". Доступно: accepted, refused.");
            return Optional.empty();
        }
        return Optional.of(scenario);
    }

    private String defaultExecutionId(CommandContext<CommandSourceStack> context) {
        String scenario = StringArgumentType.getString(context, "scenario").toLowerCase(Locale.ROOT);
        return "jdh:content_pack_" + scenario + "_" + Long.toString(System.currentTimeMillis(), 36);
    }

    private static String dialogueSessionId(String executionId) {
        return "story-action/" + executionId + "/play_greeting";
    }

    private void fail(ActiveRun run, String reason) {
        LOGGER.error("ESCPC-ACCEPTANCE FAIL scenario={} storyId={} executionId={} step={} reason={}",
                run.scenario.key, run.scenario.storyId, run.executionId, run.step, reason);
        activeRun = null;
        if ("acceptance".equals(AUTO_MODE)) {
            LOGGER.error("ESCPC-ACCEPTANCE AUTO_FAIL reason={}", reason);
            run.server.executeIfPossible(run.server::stopServer);
        }
    }

    private void sendSuccess(CommandContext<CommandSourceStack> context, String line) {
        context.getSource().sendSuccess(() -> Component.literal(line), false);
    }

    private void sendFailure(CommandContext<CommandSourceStack> context, String line) {
        context.getSource().sendFailure(Component.literal(line));
    }

    private record Scenario(String key, String storyId, String choiceId, String expectedOutcome) {
    }

    private enum Step {
        WAIT_NPC,
        WAIT_DIALOGUE_REVISION_0,
        ADVANCE_TO_SECOND_LINE,
        WAIT_DIALOGUE_REVISION_1,
        ADVANCE_TO_CHOICE,
        WAIT_DIALOGUE_REVISION_2,
        SELECT_CHOICE,
        WAIT_DIALOGUE_REVISION_3,
        ADVANCE_BRANCH_LINE,
        WAIT_DIALOGUE_TERMINAL,
        WAIT_STORY_SETTLE
    }

    private static final class ActiveRun {
        private final MinecraftServer server;
        private final Scenario scenario;
        private final String executionId;
        private final String dialogueSessionId;
        private final boolean pauseAtChoice;
        private Step step;
        private int ticks;
        private int stepTicks;

        private ActiveRun(
                MinecraftServer server,
                Scenario scenario,
                String executionId,
                Step step,
                boolean pauseAtChoice) {
            this.server = server;
            this.scenario = scenario;
            this.executionId = executionId;
            this.dialogueSessionId = dialogueSessionId(executionId);
            this.step = step;
            this.pauseAtChoice = pauseAtChoice;
        }

        static ActiveRun started(
                MinecraftServer server,
                Scenario scenario,
                String executionId,
                boolean pauseAtChoice) {
            return new ActiveRun(server, scenario, executionId, Step.WAIT_NPC, pauseAtChoice);
        }

        static ActiveRun resumed(MinecraftServer server, Scenario scenario, String executionId) {
            return new ActiveRun(server, scenario, executionId, Step.WAIT_DIALOGUE_REVISION_0, false);
        }

        static ActiveRun resumedAtChoice(MinecraftServer server, Scenario scenario, String executionId) {
            return new ActiveRun(server, scenario, executionId, Step.WAIT_DIALOGUE_REVISION_2, false);
        }

        String summary() {
            return "scenario=" + scenario.key + ", storyId=" + scenario.storyId + ", executionId="
                    + executionId + ", step=" + step + ", sessionId=" + dialogueSessionId;
        }
    }
}
