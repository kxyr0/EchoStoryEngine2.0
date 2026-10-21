package io.github.kxyr0.echoplayer.core.dialogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

final class PlayerDialogueCallBinderTest {
    private final PlayerDialogueCallBinder binder = new PlayerDialogueCallBinder();

    @Test
    void bindsSetPlayerNameFromEchoScriptAst() {
        var result = binder.bindSetPlayerName(firstCall("""
                namespace EchoContentPlayer.Test;
                class DialogueNode : DialogueResource
                {
                    public void Run()
                    {
                        SetPlayerName("Лололошка");
                    }
                }
                """), "dialogue:test");

        assertTrue(result.successful());
        assertEquals("Лололошка", result.value().orElseThrow().value());
    }

    @Test
    void rejectsUnsupportedOrMalformedCalls() {
        assertFalse(binder.bindSetPlayerName(firstCall("""
                namespace EchoContentPlayer.Test;
                class DialogueNode : DialogueResource
                {
                    public void Run()
                    {
                        SetPlayerName();
                    }
                }
                """), "dialogue:test").successful());
    }

    private InvocationExpressionSyntax firstCall(String source) {
        var parse = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId("dialogue.es"), source));
        assertTrue(parse.successful(), () -> parse.diagnostics().toString());
        MethodSyntax method = parse.syntaxTree().orElseThrow().classDeclaration().members().stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .findFirst()
                .orElseThrow();
        return method.body().statements().stream()
                .filter(ExpressionStatementSyntax.class::isInstance)
                .map(ExpressionStatementSyntax.class::cast)
                .map(ExpressionStatementSyntax::expression)
                .filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
