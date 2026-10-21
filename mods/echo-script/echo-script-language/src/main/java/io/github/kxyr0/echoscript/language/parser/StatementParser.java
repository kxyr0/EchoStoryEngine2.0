package io.github.kxyr0.echoscript.language.parser;

import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.BlockStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.IfStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.SectionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.lexer.Token;
import io.github.kxyr0.echoscript.language.lexer.TokenKind;
import io.github.kxyr0.echoscript.language.parser.DefaultEchoScriptParser.TokenCursor;
import io.github.kxyr0.echoscript.language.parser.TypeParser.ParseContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class StatementParser {
    private static final Set<String> UNSUPPORTED_STATEMENTS = Set.of(
            "switch", "for", "foreach", "while", "do", "try", "throw", "await", "async"
    );

    private final TokenCursor cursor;
    private final ParseContext context;
    private final ExpressionParser expressions;

    StatementParser(TokenCursor cursor, ParseContext context, ExpressionParser expressions) {
        this.cursor = cursor;
        this.context = context;
        this.expressions = expressions;
    }

    BlockStatementSyntax parseBlock(int depth) {
        Token start = cursor.current();
        if (!cursor.match(TokenKind.OPEN_BRACE)) {
            context.expected(start, TokenKind.OPEN_BRACE);
        }
        List<StatementSyntax> statements = new ArrayList<>();
        while (!cursor.check(TokenKind.CLOSE_BRACE) && !cursor.isAtEnd()) {
            if (statements.size() >= context.limits().maxMethodStatements()) {
                context.diagnostics().report(
                        DiagnosticCodes.STATEMENT_LIMIT_EXCEEDED,
                        cursor.current().span(),
                        "Too many statements."
                );
                break;
            }
            StatementSyntax statement = parseStatement(depth + 1);
            if (statement != null) {
                statements.add(statement);
            }
        }
        if (!cursor.match(TokenKind.CLOSE_BRACE)) {
            context.missing(cursor.current(), DiagnosticCodes.MISSING_CLOSING_BRACE, "Missing closing brace.");
        }
        return new BlockStatementSyntax(statements, context.span(start, cursor.previous()));
    }

    private StatementSyntax parseStatement(int depth) {
        if (depth > context.limits().maxParserNestingDepth()) {
            context.diagnostics().report(
                    DiagnosticCodes.NESTING_LIMIT_EXCEEDED,
                    cursor.current().span(),
                    "Parser nesting limit exceeded."
            );
            synchronizeStatement();
            return null;
        }
        if (cursor.check(TokenKind.OPEN_BRACE)) {
            return parseBlock(depth + 1);
        }
        if (cursor.check(TokenKind.KEYWORD_VAR)) {
            return parseLocal();
        }
        if (cursor.check(TokenKind.KEYWORD_RETURN)) {
            return parseReturn();
        }
        if (cursor.check(TokenKind.KEYWORD_IF)) {
            return parseIf(depth + 1);
        }
        if (isUnsupportedStatement()) {
            context.diagnostics().report(
                    DiagnosticCodes.UNSUPPORTED_SYNTAX,
                    cursor.current().span(),
                    "Unsupported statement syntax."
            );
            synchronizeStatement();
            return null;
        }
        return parseExpressionStatement(depth + 1);
    }

    private StatementSyntax parseLocal() {
        Token start = cursor.consume();
        Token name = cursor.current();
        if (!cursor.match(TokenKind.IDENTIFIER)) {
            context.expected(name, TokenKind.IDENTIFIER);
        }
        Optional<ExpressionSyntax> initializer = Optional.empty();
        if (cursor.match(TokenKind.EQUALS)) {
            initializer = Optional.of(expressions.parseExpression(0));
        }
        expectSemicolon();
        return new LocalDeclarationStatementSyntax(name.text(), initializer, context.span(start, cursor.previous()));
    }

    private StatementSyntax parseReturn() {
        Token start = cursor.consume();
        Optional<ExpressionSyntax> value = Optional.empty();
        if (!cursor.check(TokenKind.SEMICOLON) && !cursor.check(TokenKind.CLOSE_BRACE)) {
            value = Optional.of(expressions.parseExpression(0));
        }
        expectSemicolon();
        return new ReturnStatementSyntax(value, context.span(start, cursor.previous()));
    }

    private StatementSyntax parseIf(int depth) {
        Token start = cursor.consume();
        if (!cursor.match(TokenKind.OPEN_PAREN)) {
            context.expected(cursor.current(), TokenKind.OPEN_PAREN);
        }
        ExpressionSyntax condition = expressions.parseExpression(depth + 1);
        if (!cursor.match(TokenKind.CLOSE_PAREN)) {
            context.missing(
                    cursor.current(),
                    DiagnosticCodes.MISSING_CLOSING_PARENTHESIS,
                    "Missing closing parenthesis."
            );
        }
        StatementSyntax thenStatement = parseRequiredBranch(depth + 1);
        Optional<StatementSyntax> elseStatement = Optional.empty();
        if (cursor.match(TokenKind.KEYWORD_ELSE)) {
            elseStatement = Optional.of(parseRequiredBranch(depth + 1));
        }
        return new IfStatementSyntax(condition, thenStatement, elseStatement, context.span(start, cursor.previous()));
    }

    private StatementSyntax parseRequiredBranch(int depth) {
        StatementSyntax branch = parseStatement(depth + 1);
        if (branch != null) {
            return branch;
        }
        return new BlockStatementSyntax(List.of(), context.emptySpan(cursor.current()));
    }

    private StatementSyntax parseExpressionStatement(int depth) {
        ExpressionSyntax expression = expressions.parseExpression(depth);
        if (cursor.check(TokenKind.OPEN_BRACE) && expression instanceof InvocationExpressionSyntax invocation) {
            BlockStatementSyntax body = parseBlock(depth + 1);
            return new SectionStatementSyntax(invocation, body, context.span(expression.span(), body.span()));
        }
        expectSemicolon();
        return new ExpressionStatementSyntax(expression, context.span(expression.span(), cursor.previous().span()));
    }

    private void expectSemicolon() {
        if (!cursor.match(TokenKind.SEMICOLON)) {
            context.missing(cursor.current(), DiagnosticCodes.MISSING_SEMICOLON, "Missing semicolon.");
            synchronizeStatement();
        }
    }

    private boolean isUnsupportedStatement() {
        return cursor.check(TokenKind.IDENTIFIER) && UNSUPPORTED_STATEMENTS.contains(cursor.current().text());
    }

    private void synchronizeStatement() {
        while (!cursor.isAtEnd()) {
            if (cursor.match(TokenKind.SEMICOLON) || cursor.check(TokenKind.CLOSE_BRACE)) {
                return;
            }
            cursor.consume();
        }
    }
}
