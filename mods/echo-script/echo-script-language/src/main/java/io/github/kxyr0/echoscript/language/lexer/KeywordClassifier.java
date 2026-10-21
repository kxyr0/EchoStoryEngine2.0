package io.github.kxyr0.echoscript.language.lexer;

final class KeywordClassifier {
    private KeywordClassifier() {
    }

    static TokenKind kind(String text) {
        return switch (text) {
            case "namespace" -> TokenKind.KEYWORD_NAMESPACE;
            case "class" -> TokenKind.KEYWORD_CLASS;
            case "public" -> TokenKind.KEYWORD_PUBLIC;
            case "private" -> TokenKind.KEYWORD_PRIVATE;
            case "protected" -> TokenKind.KEYWORD_PROTECTED;
            case "override" -> TokenKind.KEYWORD_OVERRIDE;
            case "void" -> TokenKind.KEYWORD_VOID;
            case "var" -> TokenKind.KEYWORD_VAR;
            case "new" -> TokenKind.KEYWORD_NEW;
            case "return" -> TokenKind.KEYWORD_RETURN;
            case "if" -> TokenKind.KEYWORD_IF;
            case "else" -> TokenKind.KEYWORD_ELSE;
            case "true" -> TokenKind.KEYWORD_TRUE;
            case "false" -> TokenKind.KEYWORD_FALSE;
            default -> TokenKind.IDENTIFIER;
        };
    }
}
