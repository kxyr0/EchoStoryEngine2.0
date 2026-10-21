package io.github.kxyr0.echoscript.language.parser;

import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.DialogueNodeDeclarationSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ParameterSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.TypeSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.lexer.Token;
import io.github.kxyr0.echoscript.language.lexer.TokenKind;
import io.github.kxyr0.echoscript.language.parser.DefaultEchoScriptParser.TokenCursor;
import io.github.kxyr0.echoscript.language.parser.TypeParser.ParseContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class MemberParser {
    private final TokenCursor cursor;
    private final ParseContext context;
    private final TypeParser types;
    private final ExpressionParser expressions;
    private final StatementParser statements;

    MemberParser(TokenCursor cursor, ParseContext context, TypeParser types, ExpressionParser expressions,
            StatementParser statements) {
        this.cursor = cursor;
        this.context = context;
        this.types = types;
        this.expressions = expressions;
        this.statements = statements;
    }

    List<MemberSyntax> parseMembers(boolean dialogueResource) {
        List<MemberSyntax> members = new ArrayList<>();
        while (!cursor.check(TokenKind.CLOSE_BRACE) && !cursor.isAtEnd()) {
            if (members.size() >= context.limits().maxClassMembers()) {
                reportMemberLimit();
                break;
            }
            MemberSyntax member = parseMember(dialogueResource);
            if (member != null) {
                members.add(member);
            }
        }
        return members;
    }

    private MemberSyntax parseMember(boolean dialogueResource) {
        Token start = cursor.current();
        List<SyntaxModifier> modifiers = parseContextualModifiers();
        if (dialogueResource && isDialogueNodeStart()) {
            return parseDialogueNode(modifiers, start);
        }
        if (modifiers.isEmpty() && cursor.check(TokenKind.IDENTIFIER) && cursor.peek(1).is(TokenKind.OPEN_PAREN)) {
            return parseClassIntent(start);
        }
        TypeSyntax type = types.parseType();
        Token name = expectIdentifier("Expected member name.");
        if (cursor.match(TokenKind.OPEN_PAREN)) {
            return parseMethod(modifiers, type, name, start);
        }
        if (!name.is(TokenKind.IDENTIFIER)) {
            context.diagnostics().report(DiagnosticCodes.MALFORMED_FIELD, start.span(), "Malformed field declaration.");
        }
        return parseField(modifiers, type, name, start);
    }

    private DialogueNodeDeclarationSyntax parseDialogueNode(List<SyntaxModifier> modifiers, Token start) {
        boolean entry = modifiers.contains(SyntaxModifier.START);
        cursor.consume();
        Token name = expectIdentifier("Expected dialogue node name.");
        if (!cursor.match(TokenKind.OPEN_PAREN)) {
            context.expected(cursor.current(), TokenKind.OPEN_PAREN);
        }
        if (!cursor.match(TokenKind.CLOSE_PAREN)) {
            context.missing(cursor.current(), DiagnosticCodes.MISSING_CLOSING_PARENTHESIS,
                    "Missing closing parenthesis in dialogue node declaration.");
        }
        BlockStatementSyntax body = statements.parseBlock(0);
        return new DialogueNodeDeclarationSyntax(modifiers, entry, name.text(), body,
                context.span(start, cursor.previous()));
    }

    private MemberSyntax parseClassIntent(Token start) {
        ExpressionSyntax expression = expressions.parseExpression(0);
        if (cursor.check(TokenKind.OPEN_BRACE)) {
            BlockStatementSyntax body = statements.parseBlock(0);
            if (expression instanceof InvocationExpressionSyntax call) {
                return new ClassSectionSyntax(call, body, context.span(start, cursor.previous()));
            }
            context.diagnostics().report(DiagnosticCodes.MALFORMED_FIELD, start.span(),
                    "Malformed class-level section.");
            return null;
        }
        if (!cursor.match(TokenKind.SEMICOLON)) {
            context.missing(cursor.current(), DiagnosticCodes.MISSING_SEMICOLON,
                    "Missing semicolon after class-level declaration.");
        }
        if (expression instanceof InvocationExpressionSyntax call) {
            return new ClassCallSyntax(call, context.span(start, cursor.previous()));
        }
        context.diagnostics().report(DiagnosticCodes.MALFORMED_FIELD, start.span(),
                "Malformed class-level declaration.");
        return null;
    }

    private boolean isDialogueNodeStart() {
        return cursor.current().text().equals("Node")
                && cursor.peek(1).is(TokenKind.IDENTIFIER) && cursor.peek(2).is(TokenKind.OPEN_PAREN);
    }

    private List<SyntaxModifier> parseContextualModifiers() {
        List<SyntaxModifier> modifiers = new ArrayList<>(types.parseModifiers());
        if (cursor.current().text().equals("start")) {
            modifiers.add(SyntaxModifier.START);
            cursor.consume();
        }
        return List.copyOf(modifiers);
    }

    private FieldSyntax parseField(List<SyntaxModifier> modifiers, TypeSyntax type, Token name, Token start) {
        Optional<ExpressionSyntax> initializer = Optional.empty();
        if (cursor.match(TokenKind.EQUALS)) {
            initializer = Optional.of(expressions.parseExpression(0));
        }
        if (!cursor.match(TokenKind.SEMICOLON)) {
            context.diagnostics().report(DiagnosticCodes.MALFORMED_FIELD, start.span(), "Malformed field declaration.");
            context.missing(cursor.current(), DiagnosticCodes.MISSING_SEMICOLON, "Missing semicolon after field.");
            synchronizeMember();
        }
        return new FieldSyntax(modifiers, type, name.text(), initializer, context.span(start, cursor.previous()));
    }

    private MethodSyntax parseMethod(List<SyntaxModifier> modifiers, TypeSyntax returnType, Token name, Token start) {
        List<ParameterSyntax> parameters = parseParameters();
        if (!cursor.check(TokenKind.OPEN_BRACE)) {
            context.diagnostics().report(DiagnosticCodes.MALFORMED_METHOD, name.span(),
                    "Malformed method declaration.");
        }
        BlockStatementSyntax body = statements.parseBlock(0);
        return new MethodSyntax(modifiers, returnType, name.text(), parameters, body,
                context.span(start, cursor.previous()));
    }

    private List<ParameterSyntax> parseParameters() {
        List<ParameterSyntax> parameters = new ArrayList<>();
        while (!cursor.check(TokenKind.CLOSE_PAREN) && !cursor.isAtEnd()) {
            Token start = cursor.current();
            TypeSyntax type = types.parseType();
            Token name = expectIdentifier("Expected parameter name.");
            parameters.add(new ParameterSyntax(type, name.text(), context.span(start, name)));
            if (!cursor.match(TokenKind.COMMA)) {
                break;
            }
        }
        if (!cursor.match(TokenKind.CLOSE_PAREN)) {
            context.missing(cursor.current(), DiagnosticCodes.MISSING_CLOSING_PARENTHESIS,
                    "Missing closing parenthesis.");
        }
        return parameters;
    }

    private Token expectIdentifier(String message) {
        Token token = cursor.current();
        if (cursor.match(TokenKind.IDENTIFIER)) {
            return token;
        }
        context.diagnostics().report(DiagnosticCodes.EXPECTED_TOKEN, token.span(), message);
        return token;
    }

    private void reportMemberLimit() {
        context.diagnostics().report(DiagnosticCodes.MEMBER_LIMIT_EXCEEDED, cursor.current().span(),
                "Too many class members.");
    }

    private void synchronizeMember() {
        while (!cursor.isAtEnd()) {
            if (cursor.match(TokenKind.SEMICOLON) || cursor.check(TokenKind.CLOSE_BRACE)) {
                return;
            }
            if (cursor.current().kind().isModifier()) {
                return;
            }
            cursor.consume();
        }
    }
}
