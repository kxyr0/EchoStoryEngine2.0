package io.github.kxyr0.echodialogue.forge.gametest;

import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.candidate;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.required;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.validDialogue;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprinter;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import io.github.kxyr0.echodialogue.forge.EchoDialogueMod;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.persistence.recovery.DialogueRecoveryPlan;
import io.github.kxyr0.echodialogue.persistence.recovery.DialogueRecoveryPlanner;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoDialogueMod.MOD_ID)
public final class ForgeDialogueVisualGameTests {
    static final String COMPILE_TEST = "ForgeDialogueVisualGameTests.visualAliasesCompile";
    static final String CATALOG_TEST = "ForgeDialogueVisualGameTests.invalidVisualRetainsCatalog";
    static final String PRESENTATION_TEST = "ForgeDialogueVisualGameTests.presentationsTransferVisualDescriptors";
    static final String RECOVERY_TEST = "ForgeDialogueVisualGameTests.changedVisualBlocksRecovery";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgeDialogueVisualGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void visualAliasesCompile(GameTestHelper helper) {
        mark(COMPILE_TEST);
        CompiledDialogue portrait = candidate("visual:portrait.dialogue.es",
                ForgeDialogueVisualGameTestSources.portraitSource()).dialogues().get(0);
        CompiledDialogue skin = visualChoiceDialogue();

        helper.assertTrue(portrait.lines().get(0).speakerVisual().orElseThrow().kind()
                == SpeakerVisualKind.PORTRAIT, "portrait visual must compile");
        helper.assertTrue(skin.lines().get(0).speakerVisual().orElseThrow().kind()
                == SpeakerVisualKind.SKIN_HEAD, "skin-head visual must compile");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void invalidVisualRetainsCatalog(GameTestHelper helper) {
        mark(CATALOG_TEST);
        DialogueCatalogCandidate invalid = candidate("visual:invalid.dialogue.es",
                ForgeDialogueVisualGameTestSources.portraitSource()
                        .replace("Portrait(\"echo_dialogue_test:father_portrait\")",
                                "Icon(\"echo_dialogue_test:father_portrait\")"));
        DialogueCatalogState state = new DialogueCatalogState();
        state.install(new DialogueCatalogCandidate(List.of(validDialogue()), List.of()));
        int revision = state.current().revision();
        var rejected = state.install(invalid);

        helper.assertTrue(!rejected.installed(), "invalid visual candidate must be rejected");
        helper.assertTrue(state.current().revision() == revision, "catalog revision must not advance");
        helper.assertTrue(invalid.dialogues().isEmpty(), "invalid visual source must not compile");
        helper.assertTrue(invalid.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("EDLG-SEM-031")), "invalid visual diagnostic must be present");
        helper.assertTrue(state.current().find(DialogueId.parse("sample:greeting")).isPresent(),
                "existing known-good dialogue remains available");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void presentationsTransferVisualDescriptors(GameTestHelper helper) {
        mark(PRESENTATION_TEST);
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        DialogueSessionId sessionId = new DialogueSessionId("visual-session");
        var started = sessions.start(new AudienceId("visual-audience"), sessionId, visualChoiceDialogue());
        var choice = required(sessions.apply(new AudienceId("visual-audience"),
                DialogueInput.advance(sessionId, 0)));

        helper.assertTrue(started.presentation().orElseThrow().speakerVisual().isPresent(),
                "line presentation must expose visual descriptor");
        helper.assertTrue(choice.presentation().orElseThrow().kind() == DialoguePresentationKind.CHOICE,
                "choice presentation must open");
        helper.assertTrue(choice.presentation().orElseThrow().speakerVisual().orElseThrow().kind()
                == SpeakerVisualKind.SKIN_HEAD, "choice prompt visual must be explicit");
        helper.assertTrue(validDialogue().lines().get(1).speakerVisual().isEmpty(),
                "speaker without visual must transfer no descriptor");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void changedVisualBlocksRecovery(GameTestHelper helper) {
        mark(RECOVERY_TEST);
        CompiledDialogue original = visualChoiceDialogue();
        DialogueSessionId sessionId = new DialogueSessionId("visual-recovery-session");
        var state = new DialogueSessionEngine().start(original, sessionId, new AudienceId("visual-audience")).state();
        DialogueCheckpoint checkpoint = DialogueCheckpoint.active(
                state, original, CompiledDialogueFingerprinter.fingerprint(original), 1000);
        CompiledDialogue changed = candidate("visual:changed.dialogue.es",
                ForgeDialogueVisualGameTestSources.skinHeadChoiceSource()
                        .replace("echo_dialogue_test:merchant_skin", "echo_dialogue_test:merchant_skin_alt"))
                .dialogues().get(0);

        DialogueRecoveryPlan plan = new DialogueRecoveryPlanner().plan(
                checkpoint, Map.of(checkpoint.dialogueId(), changed));

        helper.assertTrue(plan.kind() == DialogueRecoveryPlan.Kind.BLOCKED,
                "changed visual ID must block recovery");
        helper.succeed();
    }

    private static CompiledDialogue visualChoiceDialogue() {
        return candidate("visual:skin.dialogue.es", ForgeDialogueVisualGameTestSources.skinHeadChoiceSource())
                .dialogues()
                .get(0);
    }

    private static void mark(String testName) {
        LOGGER.info("EDLG-GAMETEST-RUN {}", testName);
    }
}
