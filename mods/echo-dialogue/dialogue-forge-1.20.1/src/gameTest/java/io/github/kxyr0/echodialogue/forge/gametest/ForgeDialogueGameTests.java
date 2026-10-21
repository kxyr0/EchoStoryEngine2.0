package io.github.kxyr0.echodialogue.forge.gametest;

import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.assertCategory;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.assertClosed;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.assertCursor;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.assertOutcome;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.assertPresentation;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.candidate;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.choiceDialogue;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.required;
import static io.github.kxyr0.echodialogue.forge.gametest.ForgeDialogueGameTestSupport.validDialogue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import io.github.kxyr0.echodialogue.forge.EchoDialogueMod;
import io.github.kxyr0.echodialogue.forge.catalog.CatalogInstallResult;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoDialogueMod.MOD_ID)
public final class ForgeDialogueGameTests {
    static final String COMMAND_TREE_TEST = "ForgeDialogueGameTests.echoDialogueCommandsExist";
    static final String COMPILE_TEST = "ForgeDialogueGameTests.dialogueResourceCompilesInForgeRuntime";
    static final String SESSION_TEST = "ForgeDialogueGameTests.controlledAudienceSessionLifecycle";
    static final String CHOICE_TEST = "ForgeDialogueGameTests.choiceSessionBranchesToExplicitOutcome";
    static final String CATALOG_TEST = "ForgeDialogueGameTests.invalidCandidateRetainsCatalog";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgeDialogueGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void echoDialogueCommandsExist(GameTestHelper helper) {
        mark(COMMAND_TREE_TEST);
        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer()
                .getCommands()
                .getDispatcher();
        var root = dispatcher.getRoot().getChild("echo_dialogue");
        helper.assertTrue(root != null, "echo_dialogue command root must be registered");
        Set<String> children = root.getChildren().stream().map(node -> node.getName()).collect(Collectors.toSet());
        helper.assertTrue(children.equals(Set.of(
                        "health", "dialogues", "play", "active", "cancel", "providers", "provider", "diagnostics")),
                "echo_dialogue command literals must match D1 surface");
        helper.assertTrue(root.getChild("play").getChild("dialogue").getChild("player") != null,
                "play command must allow a target player for server console smoke");
        ParseResults<CommandSourceStack> parsed = dispatcher.parse(
                "echo_dialogue play sample:greeting",
                helper.getLevel().getServer().createCommandSourceStack());
        helper.assertTrue(parsed.getReader().getRemaining().isEmpty(), "namespaced DialogueId must parse");
        ParseResults<CommandSourceStack> fileName = dispatcher.parse(
                "echo_dialogue play home_arrival.es",
                helper.getLevel().getServer().createCommandSourceStack());
        helper.assertTrue(fileName.getReader().getRemaining().isEmpty(), "dialogue file name must parse");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void dialogueResourceCompilesInForgeRuntime(GameTestHelper helper) {
        mark(COMPILE_TEST);
        DialogueCatalogCandidate catalog = candidate(
                "sample:gametest.dialogue.es",
                ForgeDialogueGameTestSources.validSource());
        helper.assertTrue(catalog.diagnostics().stream().noneMatch(diagnostic -> diagnostic.isError()),
                "valid DialogueResource must have no error diagnostics");
        helper.assertTrue(catalog.dialogues().size() == 1, "one dialogue must compile");
        CompiledDialogue dialogue = catalog.dialogues().get(0);
        helper.assertTrue(dialogue.lines().get(0).text().equals("Где ты был?"), "Russian text must be preserved");
        helper.assertTrue(dialogue.lines().get(0).speakerDisplayName().equals("Отец"), "display name must compile");
        helper.assertTrue(dialogue.lines().size() == 3, "all linear lines must compile");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void controlledAudienceSessionLifecycle(GameTestHelper helper) {
        mark(SESSION_TEST);
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        CompiledDialogue dialogue = validDialogue();
        AudienceId audience = new AudienceId("gametest-controlled-audience");
        DialogueSessionId sessionId = new DialogueSessionId("gametest-session");

        DialogueTransition started = sessions.start(audience, sessionId, dialogue);
        assertOutcome(helper, started, DialogueTransitionStatus.STARTED, DialogueSessionPhase.ACTIVE);
        assertCursor(helper, started, 0, 0);
        assertPresentation(helper, started, "Отец", "Где ты был?");

        DialogueTransition second = sessions.start(audience, new DialogueSessionId("gametest-second"), dialogue);
        assertOutcome(helper, second, DialogueTransitionStatus.REJECTED, DialogueSessionPhase.ACTIVE);
        assertCursor(helper, second, 0, 0);

        DialogueTransition wrong = required(sessions.apply(
                audience, DialogueInput.advance(new DialogueSessionId("other-session"), 0)));
        assertCategory(helper, wrong, DiagnosticCategory.WRONG_PLAYER_REQUEST);
        assertOutcome(helper, wrong, DialogueTransitionStatus.REJECTED, DialogueSessionPhase.ACTIVE);
        assertCursor(helper, wrong, 0, 0);

        DialogueTransition firstAdvance = required(sessions.apply(audience, DialogueInput.advance(sessionId, 0)));
        assertOutcome(helper, firstAdvance, DialogueTransitionStatus.ADVANCED, DialogueSessionPhase.ACTIVE);
        assertCursor(helper, firstAdvance, 1, 1);
        assertPresentation(helper, firstAdvance, "Игрок", "Гулял.");

        DialogueTransition duplicate = required(sessions.apply(audience, DialogueInput.advance(sessionId, 0)));
        assertCategory(helper, duplicate, DiagnosticCategory.DUPLICATE_ADVANCE_REQUEST);
        assertOutcome(helper, duplicate, DialogueTransitionStatus.REJECTED, DialogueSessionPhase.ACTIVE);
        assertCursor(helper, duplicate, 1, 1);

        DialogueTransition stale = required(sessions.apply(audience, DialogueInput.advance(sessionId, 9)));
        assertCategory(helper, stale, DiagnosticCategory.STALE_ADVANCE_REQUEST);
        assertOutcome(helper, stale, DialogueTransitionStatus.REJECTED, DialogueSessionPhase.ACTIVE);
        assertCursor(helper, stale, 1, 1);

        DialogueTransition secondAdvance = required(sessions.apply(audience, DialogueInput.advance(sessionId, 1)));
        assertOutcome(helper, secondAdvance, DialogueTransitionStatus.ADVANCED, DialogueSessionPhase.ACTIVE);
        assertCursor(helper, secondAdvance, 2, 2);
        assertPresentation(helper, secondAdvance, "Отец", "Понятно.");

        DialogueTransition completed = required(sessions.apply(audience, DialogueInput.advance(sessionId, 2)));
        assertOutcome(helper, completed, DialogueTransitionStatus.COMPLETED, DialogueSessionPhase.COMPLETED);
        assertCursor(helper, completed, 2, 3);
        assertClosed(helper, completed, sessions);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void choiceSessionBranchesToExplicitOutcome(GameTestHelper helper) {
        mark(CHOICE_TEST);
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        CompiledDialogue dialogue = choiceDialogue();
        AudienceId audience = new AudienceId("gametest-choice-audience");
        DialogueSessionId sessionId = new DialogueSessionId("gametest-choice-session");

        DialogueTransition started = sessions.start(audience, sessionId, dialogue);
        assertOutcome(helper, started, DialogueTransitionStatus.STARTED, DialogueSessionPhase.ACTIVE);
        assertPresentation(helper, started, "Father", "Will you help me?");

        DialogueTransition choice = required(sessions.apply(audience, DialogueInput.advance(sessionId, 0)));
        assertOutcome(helper, choice, DialogueTransitionStatus.ADVANCED, DialogueSessionPhase.ACTIVE);
        helper.assertTrue(choice.presentation().orElseThrow().kind() == DialoguePresentationKind.CHOICE,
                "final question line must open a choice presentation");
        helper.assertTrue(choice.presentation().orElseThrow().choices().get(0).id().equals(new ChoiceId("accept")),
                "first visible choice id must be stable");
        helper.assertTrue(choice.presentation().orElseThrow().choices().get(1).id().equals(new ChoiceId("refuse")),
                "second visible choice id must be stable");

        DialogueTransition accepted = required(sessions.apply(audience,
                DialogueInput.selectChoice(sessionId, 1, new ChoiceId("accept"))));
        assertOutcome(helper, accepted, DialogueTransitionStatus.ADVANCED, DialogueSessionPhase.ACTIVE);
        assertCursor(helper, accepted, 0, 2);
        assertPresentation(helper, accepted, "Player", "Good.");

        DialogueTransition completed = required(sessions.apply(audience, DialogueInput.advance(sessionId, 2)));
        assertOutcome(helper, completed, DialogueTransitionStatus.COMPLETED, DialogueSessionPhase.COMPLETED);
        helper.assertTrue(completed.state().outcomeId().orElseThrow().toString().equals("sample:accepted"),
                "accepted branch must persist explicit terminal outcome");
        assertCursor(helper, completed, 0, 3);
        assertClosed(helper, completed, sessions);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void invalidCandidateRetainsCatalog(GameTestHelper helper) {
        mark(CATALOG_TEST);
        DialogueCatalogState state = new DialogueCatalogState();
        CompiledDialogue dialogue = validDialogue();
        state.install(new DialogueCatalogCandidate(List.of(dialogue), List.of()));
        int revision = state.current().revision();
        String invalidSource = ForgeDialogueGameTestSources.validSource()
                .replace("Line(\"player\", \"Гулял.\");", "Line(\"ghost\", \"Гулял.\");");
        CatalogInstallResult rejected = state.install(candidate("sample:invalid.dialogue.es", invalidSource));

        helper.assertTrue(!rejected.installed(), "semantic invalid candidate must be rejected");
        helper.assertTrue(state.current().revision() == revision,
                "invalid candidate must not advance catalog revision");
        helper.assertTrue(state.current().find(DialogueId.parse("sample:greeting")).isPresent(),
                "previous known-good catalog must remain visible");
        helper.assertTrue(rejected.diagnostics().stream().anyMatch(diagnostic ->
                        diagnostic.code().equals("EDLG-SEM-011") && diagnostic.subject().isPresent()),
                "invalid candidate diagnostic must include source subject");
        CatalogInstallResult fixed = state.install(new DialogueCatalogCandidate(List.of(dialogue), List.of()));
        helper.assertTrue(fixed.installed(), "fixed candidate must apply after rejected reload");
        helper.assertTrue(state.current().revision() == revision + 1, "fixed candidate must advance catalog revision");
        helper.succeed();
    }

    private static void mark(String testName) {
        LOGGER.info("EDLG-GAMETEST-RUN {}", testName);
    }
}
