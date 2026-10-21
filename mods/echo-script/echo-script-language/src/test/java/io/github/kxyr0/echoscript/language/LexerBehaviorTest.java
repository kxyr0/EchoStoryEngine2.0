package io.github.kxyr0.echoscript.language;

import io.github.kxyr0.echoscript.api.ProcessingLimits;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.lexer.EchoScriptLexer;
import io.github.kxyr0.echoscript.language.lexer.LexResult;
import io.github.kxyr0.echoscript.language.lexer.Token;
import io.github.kxyr0.echoscript.language.lexer.TokenKind;
import io.github.kxyr0.echoscript.language.source.SourceText;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerBehaviorTest {
    @Test
    void recognizesKeywordsIdentifiersPunctuationArraysIntegersDecimalsAndBooleans() {
        LexResult result = lex("namespace class public private protected override void var new return true false "
                + "Alpha.Beta[] = [42, 1.6];");

        List<TokenKind> kinds = result.tokens().stream().map(Token::kind).toList();

        assertTrue(kinds.contains(TokenKind.KEYWORD_NAMESPACE));
        assertTrue(kinds.contains(TokenKind.KEYWORD_CLASS));
        assertTrue(kinds.contains(TokenKind.KEYWORD_PUBLIC));
        assertTrue(kinds.contains(TokenKind.KEYWORD_PRIVATE));
        assertTrue(kinds.contains(TokenKind.KEYWORD_PROTECTED));
        assertTrue(kinds.contains(TokenKind.KEYWORD_OVERRIDE));
        assertTrue(kinds.contains(TokenKind.KEYWORD_VOID));
        assertTrue(kinds.contains(TokenKind.KEYWORD_VAR));
        assertTrue(kinds.contains(TokenKind.KEYWORD_NEW));
        assertTrue(kinds.contains(TokenKind.KEYWORD_RETURN));
        assertTrue(kinds.contains(TokenKind.KEYWORD_TRUE));
        assertTrue(kinds.contains(TokenKind.KEYWORD_FALSE));
        assertTrue(kinds.contains(TokenKind.IDENTIFIER));
        assertTrue(kinds.contains(TokenKind.DOT));
        assertTrue(kinds.contains(TokenKind.OPEN_BRACKET));
        assertTrue(kinds.contains(TokenKind.CLOSE_BRACKET));
        assertTrue(kinds.contains(TokenKind.INTEGER));
        assertTrue(kinds.contains(TokenKind.DECIMAL));
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void recognizesNegativeIntegersAndDecimals() {
        LexResult result = lex("Call(-7, -1.6);");

        List<String> values = result.tokens().stream()
                .filter(token -> token.is(TokenKind.INTEGER) || token.is(TokenKind.DECIMAL))
                .map(Token::value)
                .toList();

        assertEquals(List.of("-7", "-1.6"), values);
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void preservesUtf8StringsAndRecognizesEscapes() {
        LexResult result = lex("\"Стоять\" \"a\\\"b\" \"c\\\\d\" \"n\\n\\r\\t\"");

        List<String> values = result.tokens().stream()
                .filter(token -> token.is(TokenKind.STRING))
                .map(Token::value)
                .toList();

        assertEquals(List.of("Стоять", "a\"b", "c\\d", "n\n\r\t"), values);
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void skipsLineAndBlockCommentsAndTracksLfCrLfSpans() {
        LexResult lf = lex("// comment\nclass A");
        LexResult crlf = lex("/* block */\r\nclass A");

        Token lfClass = lf.tokens().stream()
                .filter(token -> token.is(TokenKind.KEYWORD_CLASS))
                .findFirst()
                .orElseThrow();
        Token crlfClass = crlf.tokens().stream()
                .filter(token -> token.is(TokenKind.KEYWORD_CLASS))
                .findFirst()
                .orElseThrow();

        assertEquals(2, lfClass.span().start().line());
        assertEquals(1, lfClass.span().start().column());
        assertEquals(2, crlfClass.span().start().line());
        assertEquals(1, crlfClass.span().start().column());
    }

    @Test
    void reportsUnexpectedCharactersInvalidEscapesAndUnterminatedText() {
        LexResult unexpected = lex("@");
        LexResult invalidEscape = lex("\"\\q\"");
        LexResult unterminatedString = lex("\"abc");
        LexResult unterminatedComment = lex("/* abc");

        assertHasCode(unexpected, DiagnosticCodes.UNEXPECTED_CHARACTER);
        assertHasCode(invalidEscape, DiagnosticCodes.INVALID_STRING_ESCAPE);
        assertHasCode(unterminatedString, DiagnosticCodes.UNTERMINATED_STRING);
        assertHasCode(unterminatedComment, DiagnosticCodes.UNTERMINATED_BLOCK_COMMENT);
    }

    @Test
    void malformedInputTerminatesWithoutExpectedExceptions() {
        assertDoesNotThrow(() -> lex("@@@ \"unterminated /* comment"));
        assertFalse(lex("@@@").tokens().isEmpty());
    }

    private static LexResult lex(String source) {
        return new EchoScriptLexer(
                new SourceText(new SourceId("lexer-test.es"), source),
                ProcessingLimits.defaults()
        ).lex();
    }

    private static void assertHasCode(LexResult result, String code) {
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }
}
