package io.github.kxyr0.echoscript.language.parser;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echoscript.api.syntax.declaration.TypeSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArrayLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.BooleanLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.DecimalLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ObjectCreationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ParenthesizedExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.lexer.Token;
import io.github.kxyr0.echoscript.language.lexer.TokenKind;
import io.github.kxyr0.echoscript.language.parser.DefaultEchoScriptParser.TokenCursor;
import io.github.kxyr0.echoscript.language.parser.TypeParser.ParseContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

final class ExpressionParser {
    private final TokenCursor cursor;
    private final ParseContext context;
    private final TypeParser typeParser;
    private final ArgumentListParser arguments;

    ExpressionParser(TokenCursor cursor, ParseContext context, TypeParser typeParser) {
        this.cursor = cursor;
        this.context = context;
        this.typeParser = typeParser;
        this.arguments = new ArgumentListParser(cursor, context, this);
    }

    ExpressionSyntax parseExpression(int depth) {
        if (depth > context.limits().maxParserNestingDepth()) {
            context.diagnostics().report(
                    DiagnosticCodes.NESTING_LIMIT_EXCEEDED,
                    cursor.current().span(),
                    "Parser nesting limit exceeded."
            );
            return errorName(cursor.current());
        }
        ExpressionSyntax expression = parsePrimary(depth);
        while (!cursor.isAtEnd()) {
            if (cursor.match(TokenKind.DOT)) {
                expression = parseMemberAccess(expression);
            } else if (cursor.match(TokenKind.OPEN_PAREN)) {
                expression = parseInvocation(expression, depth + 1);
            } else {
                return expression;
            }
        }
        return expression;
    }

    private ExpressionSyntax parsePrimary(int depth) {
        Token token = cursor.current();
        if (cursor.check(TokenKind.IDENTIFIER) && "await".equals(token.text())) {
            cursor.consume();
            return parseExpression(depth + 1);
        }
        if (cursor.match(TokenKind.IDENTIFIER)) {
            return new NameExpressionSyntax(token.text(), token.span());
        }
        if (cursor.match(TokenKind.STRING)) {
            return new StringLiteralExpressionSyntax(token.value(), token.span());
        }
        if (cursor.match(TokenKind.INTEGER)) {
            return new IntegerLiteralExpressionSyntax(parseInteger(token), token.span());
        }
        if (cursor.match(TokenKind.DECIMAL)) {
            return new DecimalLiteralExpressionSyntax(parseDecimal(token), token.span());
        }
        if (cursor.match(TokenKind.KEYWORD_TRUE) || cursor.match(TokenKind.KEYWORD_FALSE)) {
            return new BooleanLiteralExpressionSyntax(token.is(TokenKind.KEYWORD_TRUE), token.span());
        }
        if (cursor.match(TokenKind.KEYWORD_NEW)) {
            return parseObjectCreation(token, depth + 1);
        }
        if (cursor.match(TokenKind.OPEN_BRACKET)) {
            return parseArrayLiteral(token, depth + 1);
        }
        if (cursor.match(TokenKind.OPEN_PAREN)) {
            return parseParenthesized(token, depth + 1);
        }
        context.unexpected(token, "Expected expression.");
        cursor.consume();
        return errorName(token);
    }

    private ExpressionSyntax parseMemberAccess(ExpressionSyntax target) {
        Token member = cursor.current();
        if (!cursor.match(TokenKind.IDENTIFIER)) {
            context.expected(member, TokenKind.IDENTIFIER);
            return target;
        }
        return new MemberAccessExpressionSyntax(target, member.text(), context.span(target.span(), member.span()));
    }

    private ExpressionSyntax parseInvocation(ExpressionSyntax target, int depth) {
        var parsedArguments = arguments.parse(depth);
        SourceSpan end = cursor.previous().span();
        return new InvocationExpressionSyntax(target, parsedArguments, context.span(target.span(), end));
    }

    private ExpressionSyntax parseObjectCreation(Token start, int depth) {
        TypeSyntax type = typeParser.parseType();
        if (!cursor.match(TokenKind.OPEN_PAREN)) {
            context.expected(cursor.current(), TokenKind.OPEN_PAREN);
            return new ObjectCreationExpressionSyntax(type, List.of(), context.span(start, cursor.previous()));
        }
        var parsedArguments = arguments.parse(depth);
        return new ObjectCreationExpressionSyntax(type, parsedArguments, context.span(start, cursor.previous()));
    }

    private ExpressionSyntax parseArrayLiteral(Token start, int depth) {
        List<ExpressionSyntax> elements = new ArrayList<>();
        while (!cursor.check(TokenKind.CLOSE_BRACKET) && !cursor.isAtEnd()) {
            if (elements.size() >= context.limits().maxArrayElements()) {
                context.diagnostics().report(
                        DiagnosticCodes.ARRAY_ELEMENT_LIMIT_EXCEEDED,
                        cursor.current().span(),
                        "Too many array elements."
                );
                break;
            }
            elements.add(parseExpression(depth + 1));
            if (!cursor.match(TokenKind.COMMA)) {
                break;
            }
        }
        if (!cursor.match(TokenKind.CLOSE_BRACKET)) {
            context.missing(cursor.current(), DiagnosticCodes.MISSING_CLOSING_BRACKET, "Missing closing bracket.");
        }
        return new ArrayLiteralExpressionSyntax(elements, context.span(start, cursor.previous()));
    }

    private ExpressionSyntax parseParenthesized(Token start, int depth) {
        ExpressionSyntax expression = parseExpression(depth + 1);
        if (!cursor.match(TokenKind.CLOSE_PAREN)) {
            context.missing(
                    cursor.current(),
                    DiagnosticCodes.MISSING_CLOSING_PARENTHESIS,
                    "Missing closing parenthesis."
            );
        }
        return new ParenthesizedExpressionSyntax(expression, context.span(start, cursor.previous()));
    }

    private int parseInteger(Token token) {
        try {
            return Integer.parseInt(token.value());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private BigDecimal parseDecimal(Token token) {
        try {
            return new BigDecimal(token.value());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private ExpressionSyntax errorName(Token token) {
        return new NameExpressionSyntax("<error>", context.emptySpan(token));
    }
}
