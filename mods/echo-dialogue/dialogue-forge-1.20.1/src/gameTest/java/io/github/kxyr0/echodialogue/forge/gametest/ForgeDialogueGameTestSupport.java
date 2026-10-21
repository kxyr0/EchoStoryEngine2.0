package io.github.kxyr0.echodialogue.forge.gametest;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import io.github.kxyr0.echodialogue.echoscript.EchoScriptDialogueCompiler;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.resource.DialogueResourceCandidateCompiler;
import io.github.kxyr0.echodialogue.forge.resource.DialogueResourceReadResult;
import io.github.kxyr0.echodialogue.forge.resource.DialogueResourceText;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTestHelper;

final class ForgeDialogueGameTestSupport {
    private ForgeDialogueGameTestSupport() {
    }

    static DialogueCatalogCandidate candidate(String sourceId, String source) {
        return new DialogueResourceCandidateCompiler(new EchoScriptDialogueCompiler()).compile(
                new DialogueResourceReadResult(List.of(new DialogueResourceText(sourceId, source)), List.of()));
    }

    static CompiledDialogue validDialogue() {
        return candidate("sample:good.dialogue.es", ForgeDialogueGameTestSources.validSource())
                .dialogues()
                .get(0);
    }

    static CompiledDialogue choiceDialogue() {
        return candidate("sample:choice.dialogue.es", ForgeDialogueGameTestSources.choiceSource())
                .dialogues()
                .get(0);
    }

    static DialogueTransition required(Optional<DialogueTransition> transition) {
        return transition.orElseThrow(() -> new IllegalStateException("session transition must exist"));
    }

    static void assertOutcome(
            GameTestHelper helper,
            DialogueTransition transition,
            DialogueTransitionStatus status,
            DialogueSessionPhase phase
    ) {
        helper.assertTrue(transition.status() == status, "transition status must match");
        helper.assertTrue(transition.state().phase() == phase, "session phase must match");
    }

    static void assertCursor(GameTestHelper helper, DialogueTransition transition, int lineIndex, int revision) {
        helper.assertTrue(transition.state().lineIndex() == lineIndex, "line index must match");
        helper.assertTrue(transition.state().revision() == revision, "revision must match");
    }

    static void assertClosed(GameTestHelper helper, DialogueTransition transition, ActiveDialogueSessions sessions) {
        helper.assertTrue(transition.presentation().isEmpty(), "completed transition must close presentation");
        helper.assertTrue(sessions.activeCount() == 0, "completed session must not remain active");
    }

    static void assertPresentation(GameTestHelper helper, DialogueTransition transition, String speaker, String text) {
        helper.assertTrue(transition.presentation().isPresent(), "presentation must be present");
        helper.assertTrue(transition.presentation().get().speakerDisplayName().equals(speaker), "speaker must match");
        helper.assertTrue(transition.presentation().get().text().equals(text), "line text must match");
    }

    static void assertCategory(GameTestHelper helper, DialogueTransition transition, DiagnosticCategory category) {
        helper.assertTrue(transition.diagnostics().stream().anyMatch(diagnostic -> diagnostic.category() == category),
                "diagnostic category must be present");
    }

    static void assertCategory(
            GameTestHelper helper,
            List<io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic> diagnostics,
            DiagnosticCategory category
    ) {
        helper.assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.category() == category),
                "diagnostic category must be present");
    }
}
