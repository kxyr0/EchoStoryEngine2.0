package io.github.kxyr0.echoeffects.echoscript.binding;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.definition.EffectDefinitionDraft;
import io.github.kxyr0.echoeffects.api.definition.EffectEndCondition;
import io.github.kxyr0.echoeffects.api.definition.EffectPresentation;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticCode;
import io.github.kxyr0.echoeffects.api.identity.EffectDefinitionRevision;
import io.github.kxyr0.echoeffects.api.identity.EffectId;
import io.github.kxyr0.echoeffects.api.status.EffectLifetimePolicy;
import io.github.kxyr0.echoeffects.api.status.EffectStackingPolicy;
import io.github.kxyr0.echoeffects.core.definition.EffectDefinitionValidator;
import io.github.kxyr0.echoeffects.echoscript.EffectCompileResult;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EffectResourceBinder {
    private final EffectExpressionReader expressionReader = new EffectExpressionReader();
    private final EffectChannelReader channelReader = new EffectChannelReader();
    private final EffectDefinitionValidator validator = new EffectDefinitionValidator();

    public EffectCompileResult bind(CompilationUnitSyntax unit) {
        List<EffectDiagnostic> diagnostics = new ArrayList<>();
        ClassSyntax clazz = unit.classDeclaration();
        if (!"EffectResource".equals(clazz.baseType().name().text())) {
            diagnostics.add(error("Effect resource class must extend EffectResource.", clazz.name()));
            return new EffectCompileResult(Optional.empty(), diagnostics);
        }
        EffectDefinitionDraft draft = new EffectDefinitionDraft(readId(clazz, diagnostics));
        method(clazz, "Init").ifPresent(init -> bindInit(init, draft, diagnostics));
        EffectDefinition definition = draft.toDefinition(EffectDefinitionRevision.initial());
        diagnostics.addAll(validator.validate(definition, clazz.name()).diagnostics());
        if (diagnostics.stream().anyMatch(EffectDiagnostic::isError)) {
            return new EffectCompileResult(Optional.empty(), diagnostics);
        }
        return new EffectCompileResult(Optional.of(definition), diagnostics);
    }

    private EffectId readId(ClassSyntax clazz, List<EffectDiagnostic> diagnostics) {
        Optional<MethodSyntax> build = method(clazz, "Build");
        if (build.isEmpty()) {
            diagnostics.add(error("EffectResource requires Build().", clazz.name()));
            return EffectId.of("invalid:missing_build");
        }
        return build.orElseThrow().body().statements().stream()
                .filter(ReturnStatementSyntax.class::isInstance)
                .map(ReturnStatementSyntax.class::cast)
                .flatMap(statement -> statement.value().stream())
                .filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast)
                .filter(call -> "Effect".equals(EffectCallSyntax.qualifiedName(call)))
                .findFirst()
                .flatMap(call -> EffectCallSyntax.positionalString(call, 0))
                .map(EffectId::of)
                .orElseGet(() -> {
                    diagnostics.add(error("Build() must return Effect(\"namespace:path\").", clazz.name()));
                    return EffectId.of("invalid:missing_id");
                });
    }

    private void bindInit(MethodSyntax init, EffectDefinitionDraft draft, List<EffectDiagnostic> diagnostics) {
        init.body().statements().stream()
                .flatMap(statement -> EffectCallSyntax.statementCall(statement).stream())
                .forEach(call -> bindCall(call, draft, diagnostics));
    }

    private void bindCall(InvocationExpressionSyntax call, EffectDefinitionDraft draft,
                          List<EffectDiagnostic> diagnostics) {
        String name = EffectCallSyntax.qualifiedName(call);
        try {
            switch (name) {
                case "Duration" -> draft.duration(expressionReader.duration(call));
                case "Stacking" -> draft.stacking(enumArg(call, EffectStackingPolicy.class));
                case "Lifetime" -> draft.lifetime(enumArg(call, EffectLifetimePolicy.class));
                case "Presentation" -> draft.presentation(presentation(call));
                case "EndOn" -> call.arguments().stream()
                        .map(EffectCallSyntax::nameValue)
                        .flatMap(Optional::stream)
                        .map(EffectEndCondition::valueOf)
                        .forEach(draft::addEndCondition);
                default -> {
                    if (!channelReader.read(call, draft)) {
                        diagnostics.add(error("Unknown EffectResource call: " + name, name));
                    }
                }
            }
        } catch (RuntimeException exception) {
            diagnostics.add(error(exception.getMessage(), name));
        }
    }

    private <T extends Enum<T>> T enumArg(InvocationExpressionSyntax call, Class<T> type) {
        return Enum.valueOf(type, EffectCallSyntax.nameValue(call.arguments().get(0)).orElseThrow());
    }

    private EffectPresentation presentation(InvocationExpressionSyntax call) {
        return new EffectPresentation(
                EffectCallSyntax.namedString(call, "nameKey")
                        .orElseThrow(() -> new IllegalArgumentException("Presentation requires nameKey.")),
                EffectCallSyntax.namedString(call, "descriptionKey")
                        .orElseThrow(() -> new IllegalArgumentException("Presentation requires descriptionKey.")),
                EffectCallSyntax.namedString(call, "icon")
                        .orElseThrow(() -> new IllegalArgumentException("Presentation requires icon.")));
    }

    private Optional<MethodSyntax> method(ClassSyntax clazz, String name) {
        return clazz.members().stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .filter(method -> name.equals(method.name()))
                .findFirst();
    }

    private EffectDiagnostic error(String message, String source) {
        return EffectDiagnostic.error(EffectDiagnosticCode.EFFECT_RESOURCE_BIND_FAILED, message, source);
    }
}
