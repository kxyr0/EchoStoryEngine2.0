package io.github.kxyr0.echoscript.language.parser;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.NamespaceSyntax;
import io.github.kxyr0.echoscript.api.syntax.QualifiedNameSyntax;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.TypeSyntax;
import io.github.kxyr0.echoscript.language.diagnostic.DiagnosticCodes;
import io.github.kxyr0.echoscript.language.lexer.Token;
import io.github.kxyr0.echoscript.language.lexer.TokenKind;
import io.github.kxyr0.echoscript.language.parser.DefaultEchoScriptParser.TokenCursor;
import io.github.kxyr0.echoscript.language.parser.TypeParser.ParseContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SyntaxParser {
    private final TokenCursor cursor;
    private final ParseContext context;
    private final TypeParser types;
    private final MemberParser members;

    SyntaxParser(TokenCursor cursor, ParseContext context) {
        this.cursor = cursor;
        this.context = context;
        this.types = new TypeParser(cursor, context);
        ExpressionParser expressions = new ExpressionParser(cursor, context, types);
        StatementParser statements = new StatementParser(cursor, context, expressions);
        this.members = new MemberParser(cursor, context, types, expressions, statements);
    }

    Optional<CompilationUnitSyntax> parse() {
        Token start = cursor.current();
        Optional<NamespaceSyntax> namespace = Optional.empty();
        if (cursor.check(TokenKind.KEYWORD_NAMESPACE)) {
            namespace = Optional.of(parseNamespace());
        }
        List<ClassSyntax> classes = parseClasses();
        if (classes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CompilationUnitSyntax(namespace, classes.get(0), classes, context.span(start,
                cursor.current())));
    }

    private NamespaceSyntax parseNamespace() {
        Token start = cursor.consume();
        QualifiedNameSyntax name = types.parseQualifiedName();
        if (!cursor.match(TokenKind.SEMICOLON)) {
            context.missing(cursor.current(), DiagnosticCodes.MISSING_SEMICOLON, "Missing semicolon after namespace.");
        }
        return new NamespaceSyntax(name, context.span(start, cursor.previous()));
    }

    private ClassSyntax parseClassOrMissing() {
        List<SyntaxModifier> modifiers = types.parseModifiers();
        Token start = cursor.current();
        if (!cursor.match(TokenKind.KEYWORD_CLASS)) {
            context.diagnostics().report(DiagnosticCodes.MISSING_CLASS, start.span(), "Missing class declaration.");
            return null;
        }
        Token name = expectIdentifier("Expected class name.");
        if (!cursor.match(TokenKind.COLON)) {
            context.expected(cursor.current(), TokenKind.COLON);
        }
        TypeSyntax baseType = types.parseType();
        if (!cursor.match(TokenKind.OPEN_BRACE)) {
            context.expected(cursor.current(), TokenKind.OPEN_BRACE);
        }
        boolean dialogueResource = baseType.name().text().equals("DialogueResource") && baseType.arrayRank() == 0;
        List<MemberSyntax> classMembers = members.parseMembers(dialogueResource);
        if (!cursor.match(TokenKind.CLOSE_BRACE)) {
            context.missing(cursor.current(), DiagnosticCodes.MISSING_CLOSING_BRACE, "Missing closing brace.");
        }
        return new ClassSyntax(modifiers, name.text(), baseType, classMembers, context.span(start, cursor.previous()));
    }

    private List<ClassSyntax> parseClasses() {
        List<ClassSyntax> classes = new ArrayList<>();
        ClassSyntax classSyntax = parseClassOrMissing();
        if (classSyntax == null) {
            return List.of();
        }
        classes.add(classSyntax);
        while (!cursor.isAtEnd()) {
            if (isClassStart()) {
                ClassSyntax parsed = parseClassOrMissing();
                if (parsed != null) {
                    classes.add(parsed);
                }
            } else {
                context.unexpected(cursor.current(), "Unexpected top-level token.");
                cursor.consume();
            }
        }
        return classes;
    }

    private boolean isClassStart() {
        return cursor.check(TokenKind.KEYWORD_CLASS) || cursor.current().kind().isModifier();
    }

    private Token expectIdentifier(String message) {
        Token token = cursor.current();
        if (cursor.match(TokenKind.IDENTIFIER)) {
            return token;
        }
        context.diagnostics().report(DiagnosticCodes.EXPECTED_TOKEN, token.span(), message);
        return token;
    }

}
