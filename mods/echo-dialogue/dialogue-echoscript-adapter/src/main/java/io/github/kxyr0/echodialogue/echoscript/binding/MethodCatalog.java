package io.github.kxyr0.echodialogue.echoscript.binding;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.SyntaxNode;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ObjectCreationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Method view over one EchoScript class.
 */
public final class MethodCatalog {
    private static final List<String> SPECIAL_VOID_METHODS = List.of("Init", "CompleteEffects");

    private final List<MethodSyntax> methods;
    private final List<FieldSyntax> fields;

    private MethodCatalog(List<MethodSyntax> methods, List<FieldSyntax> fields) {
        this.methods = List.copyOf(methods);
        this.fields = List.copyOf(fields);
    }

    public static MethodCatalog from(List<MemberSyntax> members) {
        List<MethodSyntax> methods = members.stream().filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast).toList();
        List<FieldSyntax> fields = members.stream()
                .filter(FieldSyntax.class::isInstance)
                .map(FieldSyntax.class::cast)
                .toList();
        return new MethodCatalog(methods, fields);
    }

    public Optional<MethodSyntax> find(String name) {
        return methods.stream().filter(method -> method.name().equals(name)).findFirst();
    }

    public List<MethodSyntax> nodeMethods() {
        return methods.stream().filter(this::isNodeMethod).toList();
    }

    public Optional<String> fieldType(String name) {
        return fields.stream()
                .filter(field -> field.name().equals(name))
                .map(field -> field.type().name().text())
                .findFirst();
    }

    public Optional<String> fieldStringValue(String name) {
        return fields.stream()
                .filter(field -> field.name().equals(name))
                .flatMap(field -> field.initializer().stream())
                .filter(StringLiteralExpressionSyntax.class::isInstance)
                .map(StringLiteralExpressionSyntax.class::cast)
                .map(StringLiteralExpressionSyntax::value)
                .findFirst();
    }

    public Optional<Map<String, String>> fieldBlockPosition(String name) {
        return fields.stream()
                .filter(field -> field.name().equals(name))
                .flatMap(field -> field.initializer().stream())
                .filter(ObjectCreationExpressionSyntax.class::isInstance)
                .map(ObjectCreationExpressionSyntax.class::cast)
                .flatMap(creation -> blockPosition(creation).stream())
                .findFirst();
    }

    public Optional<DialogueNodeId> nodeId(MethodSyntax method, List<DialogueDiagnostic> diagnostics) {
        Optional<DialogueNodeId> explicit = readNodeIdCall(method.body().statements(), diagnostics);
        if (explicit.isPresent()) {
            return explicit;
        }
        Optional<DialogueNodeId> legacy = readNodeCreation(method.body().statements(), diagnostics);
        if (legacy.isPresent()) {
            diagnostics.add(DialogueSemanticDiagnostics.warning(
                    "DIALOGUE_LEGACY_AUTHORING_SYNTAX",
                    "Legacy new Node(...) authoring is supported for compatibility.", method));
            return legacy;
        }
        return safeNodeId(methodNodeId(method.name()), method, diagnostics);
    }

    private boolean isNodeMethod(MethodSyntax method) {
        if (method.returnType().arrayRank() != 0) {
            return false;
        }
        String returnType = method.returnType().name().text();
        if (returnType.equals("NodeDef")) {
            return true;
        }
        return returnType.equals("void") && method.modifiers().contains(SyntaxModifier.PRIVATE)
                && !SPECIAL_VOID_METHODS.contains(method.name());
    }

    private Optional<DialogueNodeId> readNodeIdCall(List<StatementSyntax> statements,
            List<DialogueDiagnostic> diagnostics) {
        for (StatementSyntax statement : statements) {
            Optional<InvocationExpressionSyntax> call = CallSyntax.statementCall(statement);
            if (call.isPresent() && CallSyntax.qualifiedName(call.orElseThrow()).equals("NodeId")) {
                return readNodeIdArgument(call.orElseThrow(), diagnostics);
            }
        }
        return Optional.empty();
    }

    private Optional<DialogueNodeId> readNodeCreation(List<StatementSyntax> statements,
            List<DialogueDiagnostic> diagnostics) {
        for (StatementSyntax statement : statements) {
            Optional<DialogueNodeId> nodeId = readNodeCreation(statement, diagnostics);
            if (nodeId.isPresent()) {
                return nodeId;
            }
        }
        return Optional.empty();
    }

    private Optional<DialogueNodeId> readNodeCreation(StatementSyntax statement,
            List<DialogueDiagnostic> diagnostics) {
        if (!(statement instanceof LocalDeclarationStatementSyntax local) || local.initializer().isEmpty()) {
            return Optional.empty();
        }
        if (!(local.initializer().orElseThrow() instanceof ObjectCreationExpressionSyntax creation)) {
            return Optional.empty();
        }
        if (!creation.type().name().text().equals("Node")) {
            return Optional.empty();
        }
        InvocationExpressionSyntax pseudoCall = new InvocationExpressionSyntax(
                creation, creation.arguments(), creation.span());
        return readNodeIdArgument(pseudoCall, diagnostics);
    }

    private Optional<Map<String, String>> blockPosition(ObjectCreationExpressionSyntax creation) {
        if (!creation.type().name().text().equals("BlockPosition") || creation.arguments().size() != 3
                || creation.arguments().stream().anyMatch(argument -> argument.name().isPresent())) {
            return Optional.empty();
        }
        Optional<String> x = blockCoordinate(creation.arguments().get(0).value());
        Optional<String> y = blockCoordinate(creation.arguments().get(1).value());
        Optional<String> z = blockCoordinate(creation.arguments().get(2).value());
        return x.isPresent() && y.isPresent() && z.isPresent()
                ? Optional.of(Map.of("x", x.orElseThrow(), "y", y.orElseThrow(), "z", z.orElseThrow()))
                : Optional.empty();
    }

    private Optional<String> blockCoordinate(ExpressionSyntax expression) {
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of(Integer.toString(literal.value()));
        }
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        return Optional.empty();
    }

    private Optional<DialogueNodeId> readNodeIdArgument(InvocationExpressionSyntax call,
            List<DialogueDiagnostic> diagnostics) {
        Optional<String> raw = CallSyntax.positionalString(call, 0);
        if (raw.isEmpty()) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-008",
                    "Node ID must be a string literal.", call));
            return Optional.empty();
        }
        return safeNodeId(raw.orElseThrow(), call, diagnostics);
    }

    private Optional<DialogueNodeId> safeNodeId(String raw, SyntaxNode node,
            List<DialogueDiagnostic> diagnostics) {
        try {
            return Optional.of(new DialogueNodeId(raw));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-015", exception.getMessage(), node));
            return Optional.empty();
        }
    }

    private static String methodNodeId(String name) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < name.length(); index++) {
            char current = name.charAt(index);
            if (Character.isUpperCase(current) && index > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }
}
