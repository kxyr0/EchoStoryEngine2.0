package io.github.kxyr0.echoscript.api.syntax;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable EchoScript V1 compilation unit.
 *
 * <p>V1 contains an optional namespace declaration and one or more class declarations when parsing succeeds.</p>
 */
public record CompilationUnitSyntax(
        Optional<NamespaceSyntax> namespaceDeclaration,
        ClassSyntax classDeclaration,
        List<ClassSyntax> classDeclarations,
        SourceSpan span
) implements SyntaxNode {
    public CompilationUnitSyntax(Optional<NamespaceSyntax> namespaceDeclaration, ClassSyntax classDeclaration,
            SourceSpan span) {
        this(namespaceDeclaration, classDeclaration, List.of(classDeclaration), span);
    }

    public CompilationUnitSyntax {
        namespaceDeclaration = Objects.requireNonNull(namespaceDeclaration, "namespaceDeclaration");
        classDeclaration = Objects.requireNonNull(classDeclaration, "classDeclaration");
        classDeclarations = List.copyOf(Objects.requireNonNull(classDeclarations, "classDeclarations"));
        span = Objects.requireNonNull(span, "span");
        if (classDeclarations.isEmpty() || !classDeclarations.get(0).equals(classDeclaration)) {
            throw new IllegalArgumentException("First class declaration must be classDeclaration.");
        }
    }
}
