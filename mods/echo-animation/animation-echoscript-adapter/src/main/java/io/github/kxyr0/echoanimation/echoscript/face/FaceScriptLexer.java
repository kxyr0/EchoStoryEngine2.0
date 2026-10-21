package io.github.kxyr0.echoanimation.echoscript.face;

import java.util.ArrayList;
import java.util.List;

/**
 * Small lexer for high-level face rig resources.
 */
final class FaceScriptLexer {
    enum Kind {
        IDENTIFIER, STRING, NUMBER, LEFT_BRACE, RIGHT_BRACE, LEFT_PAREN, RIGHT_PAREN, SEMICOLON, COMMA, DOT, EOF
    }

    record Token(Kind kind, String text, int line, int column) {
    }

    private String source;
    private int index;
    private int line;
    private int column;

    List<Token> lex(String input) {
        source = input;
        index = 0;
        line = 1;
        column = 1;
        List<Token> result = new ArrayList<>();
        while (!atEnd()) {
            skipTrivia();
            if (!atEnd()) {
                result.add(nextToken());
            }
        }
        result.add(new Token(Kind.EOF, "", line, column));
        return List.copyOf(result);
    }

    private Token nextToken() {
        char value = peek();
        if (identifierStart(value)) {
            return identifier();
        }
        if (value == '"') {
            return string();
        }
        if (numberStart(value)) {
            return number();
        }
        return punctuation();
    }

    private Token punctuation() {
        int tokenLine = line;
        int tokenColumn = column;
        char value = advance();
        return switch (value) {
            case '{' -> new Token(Kind.LEFT_BRACE, "{", tokenLine, tokenColumn);
            case '}' -> new Token(Kind.RIGHT_BRACE, "}", tokenLine, tokenColumn);
            case '(' -> new Token(Kind.LEFT_PAREN, "(", tokenLine, tokenColumn);
            case ')' -> new Token(Kind.RIGHT_PAREN, ")", tokenLine, tokenColumn);
            case ';' -> new Token(Kind.SEMICOLON, ";", tokenLine, tokenColumn);
            case ',' -> new Token(Kind.COMMA, ",", tokenLine, tokenColumn);
            case '.' -> new Token(Kind.DOT, ".", tokenLine, tokenColumn);
            default -> throw error("Unexpected character '" + value + "'.", tokenLine, tokenColumn);
        };
    }

    private Token identifier() {
        int start = index;
        int tokenLine = line;
        int tokenColumn = column;
        advance();
        while (!atEnd() && identifierPart(peek())) {
            advance();
        }
        return new Token(Kind.IDENTIFIER, source.substring(start, index), tokenLine, tokenColumn);
    }

    private Token number() {
        int start = index;
        int tokenLine = line;
        int tokenColumn = column;
        if (peek() == '-' || peek() == '+') {
            advance();
        }
        while (!atEnd() && Character.isDigit(peek())) {
            advance();
        }
        if (!atEnd() && peek() == '.') {
            advance();
            while (!atEnd() && Character.isDigit(peek())) {
                advance();
            }
        }
        return new Token(Kind.NUMBER, source.substring(start, index), tokenLine, tokenColumn);
    }

    private Token string() {
        int tokenLine = line;
        int tokenColumn = column;
        advance();
        StringBuilder value = new StringBuilder();
        while (!atEnd() && peek() != '"') {
            value.append(advanceStringChar(tokenLine, tokenColumn));
        }
        if (atEnd()) {
            throw error("Unterminated string literal.", tokenLine, tokenColumn);
        }
        advance();
        return new Token(Kind.STRING, value.toString(), tokenLine, tokenColumn);
    }

    private char advanceStringChar(int tokenLine, int tokenColumn) {
        char value = advance();
        if (value != '\\') {
            return value;
        }
        if (atEnd()) {
            throw error("Unterminated string escape.", tokenLine, tokenColumn);
        }
        char escaped = advance();
        return switch (escaped) {
            case '"', '\\', '/' -> escaped;
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> throw error("Unsupported string escape: \\" + escaped, line, column);
        };
    }

    private void skipTrivia() {
        boolean consumed;
        do {
            consumed = false;
            while (!atEnd() && Character.isWhitespace(peek())) {
                advance();
                consumed = true;
            }
            if (!atEnd() && peek() == '/' && peekNext() == '/') {
                skipLineComment();
                consumed = true;
            }
        } while (consumed);
    }

    private void skipLineComment() {
        while (!atEnd() && peek() != '\n') {
            advance();
        }
    }

    private boolean numberStart(char value) {
        return Character.isDigit(value)
                || ((value == '-' || value == '+') && Character.isDigit(peekNext()));
    }

    private boolean identifierStart(char value) {
        return Character.isLetter(value) || value == '_';
    }

    private boolean identifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    private boolean atEnd() {
        return index >= source.length();
    }

    private char peek() {
        return source.charAt(index);
    }

    private char peekNext() {
        return index + 1 < source.length() ? source.charAt(index + 1) : '\0';
    }

    private char advance() {
        char value = source.charAt(index++);
        if (value == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return value;
    }

    private IllegalArgumentException error(String message, int errorLine, int errorColumn) {
        return new IllegalArgumentException(message + " At " + errorLine + ":" + errorColumn + ".");
    }
}
