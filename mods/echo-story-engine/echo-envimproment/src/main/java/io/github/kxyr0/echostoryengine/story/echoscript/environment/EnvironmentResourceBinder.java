package io.github.kxyr0.echostoryengine.story.echoscript.environment;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.SyntaxModifier;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class EnvironmentResourceBinder {
    private static final String PROFILE = "EnvironmentResource";
    private final EnvironmentSourceDiagnostics diagnostics = new EnvironmentSourceDiagnostics();

    EnvironmentResourceBinding bind(CompilationUnitSyntax unit, String sourceId, Instant occurredAt) {
        EnvironmentSourceDiagnostics.SourceContext source =
                new EnvironmentSourceDiagnostics.SourceContext(sourceId, occurredAt);
        ClassSyntax resourceClass = unit.classDeclaration();
        if (!PROFILE.equals(resourceClass.baseType().name().text())) {
            StoryDiagnostic diagnostic = diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                    "ESE-ENV-SOURCE-010",
                    "Environment source uses unsupported resource profile.",
                    source, resourceClass.baseType().span(),
                    Map.of("profile", resourceClass.baseType().name().text())));
            return new EnvironmentResourceBinding(Optional.empty(), List.of(diagnostic));
        }
        return bindProfile(resourceClass, source);
    }

    private EnvironmentResourceBinding bindProfile(
            ClassSyntax resourceClass,
            EnvironmentSourceDiagnostics.SourceContext source) {
        List<StoryDiagnostic> values = new ArrayList<>();
        MemberIndex members = indexMembers(resourceClass, source, values);
        Optional<MethodSyntax> build = Optional.ofNullable(members.methods().get("Build"));
        if (build.isEmpty()) {
            values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                    "ESE-ENV-SOURCE-011",
                    "Environment source is missing Build method.", source, resourceClass.span(), Map.of())));
        }
        if (!values.isEmpty() || build.isEmpty()) {
            return new EnvironmentResourceBinding(Optional.empty(), values);
        }
        return evaluate(build.orElseThrow(), members, source, values);
    }

    private EnvironmentResourceBinding evaluate(
            MethodSyntax build,
            MemberIndex members,
            EnvironmentSourceDiagnostics.SourceContext source,
            List<StoryDiagnostic> values) {
        EnvironmentExpressionEvaluator evaluator =
                new EnvironmentExpressionEvaluator(source, diagnostics, members.methods(), values);
        Map<String, EnvironmentValue> fields = evaluateFields(members.fields(), evaluator);
        EnvironmentValue built = evaluator.evaluateMethod(build, fields);
        if (built instanceof EnvironmentValue.Resource resource && values.isEmpty()) {
            return new EnvironmentResourceBinding(Optional.of(resource.value()), values);
        }
        if (values.isEmpty()) {
            values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                    "ESE-ENV-SOURCE-012",
                    "Environment Build method must return Environment(...).", source, build.span(), Map.of())));
        }
        return new EnvironmentResourceBinding(Optional.empty(), values);
    }

    private Map<String, EnvironmentValue> evaluateFields(
            List<FieldSyntax> fields,
            EnvironmentExpressionEvaluator evaluator) {
        Map<String, EnvironmentValue> values = new LinkedHashMap<>();
        for (FieldSyntax field : fields) {
            EnvironmentValue value = field.initializer()
                    .map(initializer -> evaluator.evaluate(initializer, values))
                    .orElseGet(EnvironmentValue.Invalid::new);
            values.put(field.name(), value);
        }
        return values;
    }

    private MemberIndex indexMembers(
            ClassSyntax resourceClass,
            EnvironmentSourceDiagnostics.SourceContext source,
            List<StoryDiagnostic> values) {
        List<FieldSyntax> fields = new ArrayList<>();
        Map<String, MethodSyntax> methods = new LinkedHashMap<>();
        for (MemberSyntax member : resourceClass.members()) {
            if (member instanceof FieldSyntax field) {
                fields.add(field);
            } else if (member instanceof MethodSyntax method) {
                methods.put(method.name(), method);
                validateMethod(method, source, values);
            }
        }
        return new MemberIndex(fields, methods);
    }

    private void validateMethod(
            MethodSyntax method,
            EnvironmentSourceDiagnostics.SourceContext source,
            List<StoryDiagnostic> values) {
        if (method.parameters().isEmpty() && method.modifiers().contains(SyntaxModifier.PRIVATE)) {
            return;
        }
        if ("Build".equals(method.name()) && validBuildSignature(method)) {
            return;
        }
        values.add(diagnostics.error(new EnvironmentSourceDiagnostics.Issue(
                "ESE-ENV-SOURCE-013",
                "Environment source declares unsupported method.", source, method.span(),
                Map.of("method", method.name()))));
    }

    private boolean validBuildSignature(MethodSyntax method) {
        return method.parameters().isEmpty()
                && method.returnType().arrayRank() == 0
                && method.returnType().name().text().equals("EnvironmentResourceDef")
                && method.modifiers().containsAll(List.of(SyntaxModifier.PUBLIC, SyntaxModifier.OVERRIDE));
    }

    private record MemberIndex(List<FieldSyntax> fields, Map<String, MethodSyntax> methods) {
    }
}
