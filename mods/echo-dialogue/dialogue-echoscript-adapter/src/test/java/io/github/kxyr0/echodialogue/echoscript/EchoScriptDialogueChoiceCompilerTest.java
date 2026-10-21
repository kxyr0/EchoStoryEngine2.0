package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import org.junit.jupiter.api.Test;

class EchoScriptDialogueChoiceCompilerTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void choiceDialogueResourceCompilesOrderedChoicesAndOutcomes() {
        DialogueCompileResult result = compiler.compile("sample/question.dialogue.es", choiceSource());

        assertTrue(result.successful());
        CompiledDialogue dialogue = result.dialogue().orElseThrow();
        var question = dialogue.findNode(new DialogueNodeId("question")).orElseThrow();
        assertEquals(2, question.choices().size());
        assertEquals("accept", question.choices().get(0).id().toString());
        assertEquals("Да, помогу.", question.choices().get(0).text());
        assertEquals("accepted", question.choices().get(0).targetNodeId().toString());
        assertEquals("refuse", question.choices().get(1).id().toString());
        assertEquals("sample:accepted", dialogue.findNode(new DialogueNodeId("accepted"))
                .orElseThrow()
                .completion()
                .orElseThrow()
                .outcomeId()
                .toString());
    }

    @Test
    void missingChoiceTargetDoesNotCompile() {
        String source = choiceSource().replace("next: \"refused\"", "next: \"missing\"");

        DialogueCompileResult result = compiler.compile("sample/missing_choice_target.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("EDLG-SEM-027")));
    }

    @Test
    void lineAfterChoiceDoesNotCompile() {
        String source = choiceSource().replace("        return node;",
                "        Line(\"father\", \"Поздняя строка.\");\n        return node;");

        DialogueCompileResult result = compiler.compile("sample/line_after_choice.dialogue.es", source);

        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("EDLG-SEM-019")));
    }

    private static String choiceSource() {
        return sourceHeader() + questionNode() + acceptedNode() + refusedNode() + sourceFooter();
    }

    private static String sourceHeader() {
        return """
                namespace EchoDialogue.Sample;

                class QuestionDialogue : DialogueResource
                {
                    private NodeDef[] dialogueNodes = [ Question(), Accepted(), Refused() ];

                    public override DialogueResourceDef Build()
                    {
                        return Dialogue("sample:question");
                    }

                    private void Init()
                    {
                        Start("question");
                        Alias("father", "sample:father", display: "Отец");
                        Alias("player", "sample:player", display: "Игрок");
                    }

                    private NodeDef[] Nodes()
                    {
                        return dialogueNodes;
                    }

                """;
    }

    private static String questionNode() {
        return """
                    private NodeDef Question()
                    {
                        var node = new Node("question");
                        Line("father", "Ты поможешь мне?");
                        Choice("accept", "Да, помогу.", next: "accepted");
                        Choice("refuse", "Нет.", next: "refused");
                        return node;
                    }

                """;
    }

    private static String acceptedNode() {
        return """
                    private NodeDef Accepted()
                    {
                        var node = new Node("accepted");
                        Line("player", "Хорошо.");
                        Complete("sample:accepted");
                        return node;
                    }

                """;
    }

    private static String refusedNode() {
        return """
                    private NodeDef Refused()
                    {
                        var node = new Node("refused");
                        Line("player", "Мне некогда.");
                        Complete("sample:refused");
                        return node;
                    }
                """;
    }

    private static String sourceFooter() {
        return """
                }
                """;
    }
}
