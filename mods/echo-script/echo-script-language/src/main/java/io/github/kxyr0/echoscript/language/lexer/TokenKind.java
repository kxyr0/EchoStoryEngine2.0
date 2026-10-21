package io.github.kxyr0.echoscript.language.lexer;

public enum TokenKind {
    IDENTIFIER,
    INTEGER,
    DECIMAL,
    STRING,
    OPEN_BRACE,
    CLOSE_BRACE,
    OPEN_PAREN,
    CLOSE_PAREN,
    OPEN_BRACKET,
    CLOSE_BRACKET,
    COMMA,
    DOT,
    COLON,
    SEMICOLON,
    EQUALS,
    KEYWORD_NAMESPACE,
    KEYWORD_CLASS,
    KEYWORD_PUBLIC,
    KEYWORD_PRIVATE,
    KEYWORD_PROTECTED,
    KEYWORD_OVERRIDE,
    KEYWORD_VOID,
    KEYWORD_VAR,
    KEYWORD_NEW,
    KEYWORD_RETURN,
    KEYWORD_IF,
    KEYWORD_ELSE,
    KEYWORD_TRUE,
    KEYWORD_FALSE,
    EOF;

    public boolean isModifier() {
        return this == KEYWORD_PUBLIC
                || this == KEYWORD_PRIVATE
                || this == KEYWORD_PROTECTED
                || this == KEYWORD_OVERRIDE;
    }
}
