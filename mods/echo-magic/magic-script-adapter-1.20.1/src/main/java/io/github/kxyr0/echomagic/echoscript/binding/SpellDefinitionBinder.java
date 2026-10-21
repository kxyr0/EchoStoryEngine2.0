package io.github.kxyr0.echomagic.echoscript.binding;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnosticCode;
import io.github.kxyr0.echomagic.api.identity.MagicSchoolId;
import io.github.kxyr0.echomagic.api.identity.SpellDefinitionRevision;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import io.github.kxyr0.echomagic.api.gesture.MagicTechnique;
import io.github.kxyr0.echomagic.api.projectile.SpellTargetPolicy;
import io.github.kxyr0.echomagic.core.validation.SpellDefinitionValidator;
import io.github.kxyr0.echomagic.core.validation.SpellSafetyLimits;
import io.github.kxyr0.echomagic.echoscript.SpellCompileResult;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SpellDefinitionBinder {
    private final SpellDefinitionValidator validator = new SpellDefinitionValidator(SpellSafetyLimits.defaults());

    public SpellCompileResult bind(CompilationUnitSyntax unit, SpellDefinitionRevision revision) {
        List<MagicDiagnostic> diagnostics = new ArrayList<>();
        ClassSyntax clazz = unit.classDeclaration();
        if (!"SpellResource".equals(clazz.baseType().name().text())) {
            diagnostics.add(error("Spell resource class must extend SpellResource.", clazz.name()));
            return new SpellCompileResult(Optional.empty(), diagnostics);
        }
        SpellConstants constants = new SpellConstants(clazz);
        SpellValueReader values = new SpellValueReader(constants);
        SpellSectionBinder sections = new SpellSectionBinder(values);
        SpellDefinitionDraft draft = new SpellDefinitionDraft(revision);
        Optional<MethodSyntax> init = method(clazz, "Init");
        if (init.isEmpty()) diagnostics.add(error("SpellResource requires Init().", clazz.name()));
        init.ifPresent(value -> value.body().statements().stream()
                .flatMap(statement -> SpellSyntax.statementCall(statement).stream())
                .forEach(call -> bindCall(call, values, sections, draft, diagnostics)));
        try {
            SpellDefinition definition = draft.definition();
            diagnostics.addAll(validator.validate(definition, clazz.name()));
            if (diagnostics.stream().noneMatch(MagicDiagnostic::isError)) {
                return new SpellCompileResult(Optional.of(definition), diagnostics);
            }
        } catch (RuntimeException exception) {
            diagnostics.add(error(exception.getMessage(), clazz.name()));
        }
        return new SpellCompileResult(Optional.empty(), diagnostics);
    }

    private void bindCall(InvocationExpressionSyntax call, SpellValueReader values, SpellSectionBinder sections,
                          SpellDefinitionDraft draft, List<MagicDiagnostic> diagnostics) {
        String name = SpellSyntax.name(call);
        try {
            switch (name) {
                case "Id" -> draft.id(new SpellId(values.string(call, "id", 0)));
                case "DisplayName" -> draft.displayName(values.string(call, "name", 0));
                case "School" -> draft.school(new MagicSchoolId(values.string(call, "school", 0)));
                case "Technique" -> draft.technique(MagicTechnique.valueOf(values.enumName(call, "value", 0)));
                case "Cooldown" -> draft.cooldown(sections.cooldown(call));
                case "Cast" -> draft.cast(sections.cast(call));
                case "Burst" -> draft.burst(sections.burst(call));
                case "Projectile" -> draft.projectile(sections.projectile(call));
                case "Collision" -> draft.collision(sections.collision(call));
                case "Targets" -> draft.targets(SpellTargetPolicy.valueOf(values.enumName(call, "policy", 0)));
                case "FriendlyFire" -> draft.friendlyFire(values.bool(call, "enabled", 0));
                case "Trail" -> draft.trail(sections.trail(call));
                case "Core" -> draft.core(sections.core(call));
                case "Hit" -> draft.hit(sections.hit(call));
                default -> diagnostics.add(error("Unknown SpellResource call: " + name, name));
            }
        } catch (RuntimeException exception) {
            diagnostics.add(error(exception.getMessage(), name));
        }
    }

    private Optional<MethodSyntax> method(ClassSyntax clazz, String name) {
        return clazz.members().stream().filter(MethodSyntax.class::isInstance).map(MethodSyntax.class::cast)
                .filter(value -> name.equals(value.name())).findFirst();
    }

    private MagicDiagnostic error(String message, String source) {
        return MagicDiagnostic.error(MagicDiagnosticCode.MAGIC_DEFINITION_BIND_FAILED,
                message == null ? "Unknown binding failure." : message, source);
    }
}
