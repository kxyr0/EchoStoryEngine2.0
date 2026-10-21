package io.github.kxyr0.echodialogue.forge.gametest;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprinter;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryWriteResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueSessionRepository.SaveExpectation;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.forge.EchoDialogueMod;
import io.github.kxyr0.echodialogue.persistence.file.FileDialogueSessionRepository;
import io.github.kxyr0.echodialogue.persistence.recovery.DialogueRecoveryPlan;
import io.github.kxyr0.echodialogue.persistence.recovery.DialogueRecoveryPlanner;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoDialogueMod.MOD_ID)
public final class ForgeDialoguePersistenceGameTests {
    static final String DURABLE_RESTORE_TEST = "ForgeDialoguePersistenceGameTests.durableSnapshotRestoresSameLine";
    static final String INCOMPATIBLE_TEST = "ForgeDialoguePersistenceGameTests.incompatibleFingerprintBlocksRecovery";
    static final String TERMINAL_TEST = "ForgeDialoguePersistenceGameTests.terminalRecordCanBeAcknowledged";
    static final String WORLD_TEST = "ForgeDialoguePersistenceGameTests.worldRootsAreIsolated";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgeDialoguePersistenceGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void durableSnapshotRestoresSameLine(GameTestHelper helper) {
        mark(DURABLE_RESTORE_TEST);
        FileDialogueSessionRepository repository = repository(helper, "durable");
        DialogueCheckpoint checkpoint = activeSecondLine();

        repository.save(checkpoint, SaveExpectation.expectedEmpty());
        DialogueRepositoryLoadResult loaded = new FileDialogueSessionRepository(root(helper, "durable"))
                .load(checkpoint.sessionId());

        helper.assertTrue(loaded.kind() == DialogueRepositoryLoadResult.Kind.FOUND, "snapshot must load");
        DialogueCheckpoint restored = loaded.checkpoint().orElseThrow();
        helper.assertTrue(restored.sessionId().equals(checkpoint.sessionId()), "session id must match");
        helper.assertTrue(restored.revision() == 1, "revision must match second line");
        helper.assertTrue(restored.lineOrdinal() == 1, "line ordinal must match second line");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void incompatibleFingerprintBlocksRecovery(GameTestHelper helper) {
        mark(INCOMPATIBLE_TEST);
        DialogueCheckpoint checkpoint = activeSecondLine();
        DialogueRecoveryPlan plan = new DialogueRecoveryPlanner().plan(
                checkpoint,
                Map.of(checkpoint.dialogueId(), changedDialogue()));

        helper.assertTrue(plan.kind() == DialogueRecoveryPlan.Kind.BLOCKED,
                "incompatible fingerprint must block recovery");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void terminalRecordCanBeAcknowledged(GameTestHelper helper) {
        mark(TERMINAL_TEST);
        FileDialogueSessionRepository repository = repository(helper, "terminal");
        DialogueCheckpoint terminal = completed();
        DialogueCheckpoint saved = repository.save(terminal, SaveExpectation.expectedEmpty()).checkpoint()
                .orElseThrow();

        DialogueRepositoryWriteResult acknowledged = repository.acknowledgeTerminal(
                saved.sessionId(), saved.repositoryRevision());

        helper.assertTrue(acknowledged.kind() == DialogueRepositoryWriteResult.Kind.COMPLETED,
                "terminal acknowledgement must complete");
        helper.assertTrue(repository.load(saved.sessionId()).kind() == DialogueRepositoryLoadResult.Kind.NOT_FOUND,
                "acknowledged terminal record must be released");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void worldRootsAreIsolated(GameTestHelper helper) {
        mark(WORLD_TEST);
        DialogueCheckpoint checkpoint = activeSecondLine();
        repository(helper, "world-a").save(checkpoint, SaveExpectation.expectedEmpty());

        DialogueRepositoryLoadResult worldB = repository(helper, "world-b").load(checkpoint.sessionId());

        helper.assertTrue(worldB.kind() == DialogueRepositoryLoadResult.Kind.NOT_FOUND,
                "different world roots must not share sessions");
        helper.succeed();
    }

    private static DialogueCheckpoint activeSecondLine() {
        CompiledDialogue dialogue = ForgeDialogueGameTestSupport.validDialogue();
        DialogueSessionEngine engine = new DialogueSessionEngine();
        DialogueSessionId sessionId = new DialogueSessionId("gametest-durable-session");
        DialogueSessionState started = engine.start(dialogue, sessionId, new AudienceId("gametest-audience")).state();
        DialogueSessionState advanced = engine.apply(dialogue, started, DialogueInput.advance(sessionId, 0)).state();
        return DialogueCheckpoint.active(advanced, dialogue, CompiledDialogueFingerprinter.fingerprint(dialogue), 1000);
    }

    private static DialogueCheckpoint completed() {
        CompiledDialogue dialogue = ForgeDialogueGameTestSupport.validDialogue();
        DialogueSessionEngine engine = new DialogueSessionEngine();
        DialogueSessionId sessionId = new DialogueSessionId("gametest-terminal-session");
        DialogueSessionState first = engine.start(dialogue, sessionId, new AudienceId("gametest-audience")).state();
        DialogueSessionState second = engine.apply(dialogue, first, DialogueInput.advance(sessionId, 0)).state();
        DialogueSessionState third = engine.apply(dialogue, second, DialogueInput.advance(sessionId, 1)).state();
        DialogueSessionState done = engine.apply(dialogue, third, DialogueInput.advance(sessionId, 2)).state();
        return DialogueCheckpoint.terminal(done, dialogue, CompiledDialogueFingerprinter.fingerprint(dialogue), 1000,
                DialogueCheckpoint.TerminalFields.completed(done.outcomeId().orElseThrow()));
    }

    private static CompiledDialogue changedDialogue() {
        return ForgeDialogueGameTestSupport.candidate(
                "sample:changed.dialogue.es",
                ForgeDialogueGameTestSources.validSource().replace("Line(\"player\", \"Гулял.\");",
                        "Line(\"player\", \"Молчал.\");"))
                .dialogues()
                .get(0);
    }

    private static FileDialogueSessionRepository repository(GameTestHelper helper, String name) {
        return new FileDialogueSessionRepository(root(helper, name));
    }

    private static Path root(GameTestHelper helper, String name) {
        return helper.getLevel().getServer().getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("echo_dialogue")
                .resolve("gametest")
                .resolve(name);
    }

    private static void mark(String testName) {
        LOGGER.info("EDLG-GAMETEST-RUN {}", testName);
    }
}
