package io.github.kxyr0.echodialogue.echoscript.binding;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.echoscript.DialogueCompileResult;
import io.github.kxyr0.echodialogue.echoscript.api.DialogueLineArgumentBinder;
import io.github.kxyr0.echodialogue.echoscript.binding.highlevel.HighLevelDialogueBinder;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echodialogue.echoscript.resource.SourceResourceContext;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.DialogueNodeDeclarationSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Selects one deterministic DialogueResource authoring mode before semantic lowering.
 */
public final class DialogueResourceBinder {
    private final LegacyDialogueBinder legacy;
    private final HighLevelDialogueBinder highLevel;

    public DialogueResourceBinder() {
        this(List.of());
    }

    public DialogueResourceBinder(List<DialogueLineArgumentBinder> extensions) {
        legacy = new LegacyDialogueBinder(extensions);
        highLevel = new HighLevelDialogueBinder(extensions);
    }

    public DialogueCompileResult bind(CompilationUnitSyntax unit) {
        return bind(unit, Optional.empty());
    }

    public DialogueCompileResult bind(CompilationUnitSyntax unit, Optional<SourceResourceContext> sourceContext) {
        List<DialogueDiagnostic> diagnostics = new ArrayList<>();
        ClassSyntax resource = unit.classDeclaration();
        if (!resource.baseType().name().text().equals("DialogueResource")) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-001",
                    "Source must contain class ... : DialogueResource."));
            return new DialogueCompileResult(Optional.empty(), diagnostics);
        }
        if (isHighLevel(resource)) {
            long resources = unit.classDeclarations().stream().filter(this::isHighLevel).count();
            if (resources > 1) {
                diagnostics.add(DialogueSemanticDiagnostics.semantic("ECHO_SCRIPT_MULTIPLE_INFERRED_RESOURCES",
                        "A high-level EchoScript file must contain one inferred DialogueResource."));
                return new DialogueCompileResult(Optional.empty(), diagnostics);
            }
            return highLevel.bind(unit, resource, sourceContext);
        }
        return legacy.bind(unit);
    }

    private boolean isHighLevel(ClassSyntax resource) {
        if (resource.members().stream().anyMatch(DialogueNodeDeclarationSyntax.class::isInstance)) {
            return true;
        }
        return resource.baseType().name().text().equals("DialogueResource")
                && resource.members().stream().filter(MethodSyntax.class::isInstance).map(MethodSyntax.class::cast)
                        .noneMatch(method -> List.of("Build", "Init", "Nodes").contains(method.name())
                                || method.returnType().name().text().equals("NodeDef"));
    }
}
