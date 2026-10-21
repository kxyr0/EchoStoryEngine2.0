package io.github.kxyr0.echodialogue.core.fingerprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueChoice;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueCompletion;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompiledDialogueFingerprinterTest {
    @Test
    void equivalentCompiledMeaningHasSameFingerprint() {
        assertEquals(fingerprint(dialogue("Где ты был?", "Гулял.")),
                fingerprint(dialogue("Где ты был?", "Гулял.")));
    }

    @Test
    void SemanticTextOrOrderChangeChangesFingerprint() {
        CompiledDialogueFingerprint original = fingerprint(dialogue("Где ты был?", "Гулял."));

        assertNotEquals(original, fingerprint(dialogue("Где ты был?", "Молчал.")));
        assertNotEquals(original, fingerprint(dialogue("Гулял.", "Где ты был?")));
    }

    @Test
    void choiceAndOutcomeChangesChangeFingerprint() {
        CompiledDialogueFingerprint original = fingerprint(choiceDialogue("Да, помогу.", "accepted",
                "sample:accepted"));

        assertNotEquals(original, fingerprint(choiceDialogue("Конечно.", "accepted", "sample:accepted")));
        assertNotEquals(original, fingerprint(choiceDialogue("Да, помогу.", "refused", "sample:accepted")));
        assertNotEquals(original, fingerprint(choiceDialogue("Да, помогу.", "accepted", "sample:other")));
    }

    @Test
    void visualMeaningChangesFingerprintButNotAssetBytes() {
        CompiledDialogueFingerprint textOnly = fingerprint(dialogue(Optional.empty()));
        CompiledDialogueFingerprint portrait = fingerprint(dialogue(Optional.of(
                SpeakerVisualRef.portrait("sample:father"))));

        assertNotEquals(textOnly, portrait);
        assertNotEquals(portrait, fingerprint(dialogue(Optional.of(
                SpeakerVisualRef.skinHead("sample:father")))));
        assertNotEquals(portrait, fingerprint(dialogue(Optional.of(
                SpeakerVisualRef.portrait("sample:merchant")))));
        assertEquals(portrait, fingerprint(dialogue(Optional.of(
                SpeakerVisualRef.portrait("sample:father")))));
    }

    @Test
    void speakerNameColorChangesFingerprintWhenAuthored() {
        CompiledDialogueFingerprint textOnly = fingerprint(coloredDialogue(Optional.empty()));
        CompiledDialogueFingerprint blue = fingerprint(coloredDialogue(Optional.of(
                SpeakerNameColor.parse("#57C7FF"))));

        assertNotEquals(textOnly, blue);
        assertNotEquals(blue, fingerprint(coloredDialogue(Optional.of(SpeakerNameColor.parse("#FFCC00")))));
        assertEquals(blue, fingerprint(coloredDialogue(Optional.of(SpeakerNameColor.parse("#57C7FF")))));
    }

    private static CompiledDialogueFingerprint fingerprint(CompiledDialogue dialogue) {
        return CompiledDialogueFingerprinter.fingerprint(dialogue);
    }

    private static CompiledDialogue dialogue(String first, String second) {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Отец", first),
                new CompiledDialogueLine(nodeId, 1, SpeakerId.parse("sample:player"), "Игрок", second)
        ));
    }

    private static CompiledDialogue dialogue(Optional<SpeakerVisualRef> visual) {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:visual"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Отец", visual,
                        "Где ты был?")
        ));
    }

    private static CompiledDialogue coloredDialogue(Optional<SpeakerNameColor> color) {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:color"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:player"), "РРіСЂРѕРє", color,
                        Optional.empty(), "Р“СѓР»СЏР».", Optional.empty(), false, List.of())
        ));
    }

    private static CompiledDialogue choiceDialogue(String choiceText, String target, String outcome) {
        DialogueNodeId question = new DialogueNodeId("question");
        DialogueNodeId targetNode = new DialogueNodeId(target);
        return new CompiledDialogue(DialogueId.parse("sample:question"), question, List.of(
                new CompiledDialogueNode(question, List.of(line(question, 0, "Ты поможешь мне?")), List.of(
                        new CompiledDialogueChoice(new ChoiceId("accept"), choiceText, targetNode)
                ), Optional.empty()),
                new CompiledDialogueNode(targetNode, List.of(line(targetNode, 0, "Хорошо.")), List.of(),
                        Optional.of(new CompiledDialogueCompletion(DialogueOutcomeId.parse(outcome))))
        ));
    }

    private static CompiledDialogueLine line(DialogueNodeId nodeId, int index, String text) {
        return new CompiledDialogueLine(nodeId, index, SpeakerId.parse("sample:father"), "Отец", text);
    }
}
