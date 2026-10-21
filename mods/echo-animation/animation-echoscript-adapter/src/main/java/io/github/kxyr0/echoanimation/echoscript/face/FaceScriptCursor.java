package io.github.kxyr0.echoanimation.echoscript.face;

import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Vec2;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import java.util.List;

/**
 * Token cursor and typed call readers for high-level face rig syntax.
 */
final class FaceScriptCursor {
    private final List<FaceScriptLexer.Token> tokens;
    private int index;

    FaceScriptCursor(String source) {
        tokens = new FaceScriptLexer().lex(source);
    }

    void parseNamespace() {
        if (!peekIdentifier("namespace")) {
            return;
        }
        advance();
        identifier();
        while (match(FaceScriptLexer.Kind.DOT)) {
            identifier();
        }
        expect(FaceScriptLexer.Kind.SEMICOLON, "Expected ';' after namespace.");
    }

    Vec3 positionCall() {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after Position.");
        Vec3 result = new Vec3(number(), commaNumber(), commaNumber());
        expectEndCall("Position");
        return result;
    }

    Vec3 pivotCall() {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after Pivot.");
        Vec3 result = new Vec3(number(), commaNumber(), 0.0D);
        expectEndCall("Pivot");
        return result;
    }

    Vec2 vec2Call(String name) {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after " + name + ".");
        Vec2 result = new Vec2(number(), commaNumber());
        expectEndCall(name);
        return result;
    }

    IntRect textureAreaCall() {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after TextureArea.");
        IntRect rect = new IntRect(integer(), commaInteger(), commaInteger(), commaInteger());
        expectEndCall("TextureArea");
        return rect;
    }

    FaceScriptModel.Size sizeCall() {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after Size.");
        FaceScriptModel.Size result = new FaceScriptModel.Size(integer(), commaInteger());
        expectEndCall("Size");
        return result;
    }

    FaceScriptModel.Timing timingCall() {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after Timing.");
        var result = new FaceScriptModel.Timing(integer(), commaInteger(), commaInteger(), commaInteger());
        expectEndCall("Timing");
        return result;
    }

    String stringCall(String name) {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after " + name + ".");
        String value = expect(FaceScriptLexer.Kind.STRING, name + " expects a string.").text();
        expectEndCall(name);
        return value;
    }

    int intCall(String name) {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after " + name + ".");
        int value = integer();
        expectEndCall(name);
        return value;
    }

    boolean boolCall(String name) {
        expect(FaceScriptLexer.Kind.LEFT_PAREN, "Expected '(' after " + name + ".");
        String value = identifier();
        expectEndCall(name);
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw error(name + " expects true or false.");
        };
    }

    void expectEnd() {
        expect(FaceScriptLexer.Kind.EOF, "Unexpected tokens after face resource.");
    }

    void expectIdentifier(String text) {
        String actual = identifier();
        if (!text.equals(actual)) {
            throw error("Expected '" + text + "', got '" + actual + "'.");
        }
    }

    String identifier() {
        return expect(FaceScriptLexer.Kind.IDENTIFIER, "Expected identifier.").text();
    }

    boolean peekIdentifier(String text) {
        return peek().kind() == FaceScriptLexer.Kind.IDENTIFIER && text.equals(peek().text());
    }

    boolean match(FaceScriptLexer.Kind kind) {
        if (peek().kind() != kind) {
            return false;
        }
        advance();
        return true;
    }

    FaceScriptLexer.Token expect(FaceScriptLexer.Kind kind, String message) {
        if (peek().kind() == kind) {
            return advance();
        }
        throw error(message);
    }

    IllegalArgumentException error(String message) {
        FaceScriptLexer.Token token = peek();
        return new IllegalArgumentException(message + " At " + token.line() + ":" + token.column() + ".");
    }

    private void expectEndCall(String name) {
        expect(FaceScriptLexer.Kind.RIGHT_PAREN, "Expected ')' after " + name + " arguments.");
        expect(FaceScriptLexer.Kind.SEMICOLON, "Expected ';' after " + name + " call.");
    }

    private double commaNumber() {
        expect(FaceScriptLexer.Kind.COMMA, "Expected ','.");
        return number();
    }

    private int commaInteger() {
        expect(FaceScriptLexer.Kind.COMMA, "Expected ','.");
        return integer();
    }

    private double number() {
        String text = expect(FaceScriptLexer.Kind.NUMBER, "Expected number.").text();
        return Double.parseDouble(text);
    }

    private int integer() {
        double value = number();
        if (value != Math.rint(value)) {
            throw error("Expected integer value.");
        }
        return (int) value;
    }

    private FaceScriptLexer.Token advance() {
        return tokens.get(index++);
    }

    private FaceScriptLexer.Token peek() {
        return tokens.get(index);
    }
}
