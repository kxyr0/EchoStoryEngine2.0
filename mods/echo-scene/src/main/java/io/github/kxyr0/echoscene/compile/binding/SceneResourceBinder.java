package io.github.kxyr0.echoscene.compile.binding;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.SectionStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import io.github.kxyr0.echoscene.api.definition.SceneCommand;
import io.github.kxyr0.echoscene.api.definition.SceneDefinition;
import io.github.kxyr0.echoscene.api.definition.SceneStage;
import io.github.kxyr0.echoscene.api.descriptor.SceneDescriptor;
import io.github.kxyr0.echoscene.api.descriptor.SceneReference;
import io.github.kxyr0.echoscene.api.diagnostic.SceneDiagnostic;
import io.github.kxyr0.echoscene.api.identity.SceneLocalId;
import io.github.kxyr0.echoscene.api.identity.SceneResourceId;
import io.github.kxyr0.echoscene.api.identity.SceneStepId;
import io.github.kxyr0.echoscene.api.model.SceneRevision;
import io.github.kxyr0.echoscene.compile.SceneCommandRegistry;
import io.github.kxyr0.echoscene.compile.SceneSymbolCatalog;
import io.github.kxyr0.echoscene.compile.source.SceneSourceInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SceneResourceBinder {
    private final SceneEntrySelector entries = new SceneEntrySelector();
    private final SceneIdInference ids = new SceneIdInference();
    private final SceneCommandBinder commandBinder;

    public SceneResourceBinder(SceneSymbolCatalog symbols, SceneCommandRegistry commands) {
        commandBinder = new SceneCommandBinder(symbols, commands);
    }

    public BindingResult bind(CompilationUnitSyntax unit, SceneSourceInput input) {
        ClassSyntax resource = unit.classDeclaration();
        List<SceneDiagnostic> diagnostics = new ArrayList<>();
        if (!resource.baseType().name().text().equals("SceneResource")) {
            diagnostics.add(error("SCENE_RESOURCE_NOT_FOUND", "Expected SceneResource profile.", resource.span()));
            return failed(diagnostics);
        }
        List<MethodSyntax> scenes = resource.members().stream().filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast).filter(method -> method.returnType().name().text().equals("Scene"))
                .toList();
        Optional<MethodSyntax> entry = entries.select(scenes);
        if (entry.isEmpty()) {
            String code = entries.ambiguous(scenes) ? "SCENE_ENTRY_AMBIGUOUS" : "SCENE_ENTRY_MISSING";
            diagnostics.add(error(code, "SceneResource requires exactly one entry Scene.", resource.span()));
            return failed(diagnostics);
        }
        return bindEntry(input, scenes, entry.orElseThrow(), diagnostics);
    }

    private BindingResult bindEntry(SceneSourceInput input, List<MethodSyntax> scenes, MethodSyntax entry,
            List<SceneDiagnostic> diagnostics) {
        List<SceneStage> stages = new ArrayList<>();
        List<SceneReference> references = new ArrayList<>();
        int index = 0;
        for (StatementSyntax statement : entry.body().statements()) {
            Optional<SceneStage> stage = stage(statement, index, references, diagnostics);
            stage.ifPresent(stages::add);
            index++;
        }
        if (!diagnostics.isEmpty()) {
            return failed(diagnostics);
        }
        SceneResourceId resourceId = ids.resource(input.contentNamespace(), input.sourcePath());
        SceneLocalId entryId = ids.local(entry.name());
        SceneDefinition definition = new SceneDefinition(resourceId, entryId, new SceneRevision(input.revision()), stages);
        SceneDescriptor descriptor = new SceneDescriptor(resourceId, entryId,
                scenes.stream().map(method -> ids.local(method.name())).toList(), references,
                definition.capabilityPlan());
        return new BindingResult(Optional.of(definition), Optional.of(descriptor), diagnostics);
    }

    private Optional<SceneStage> stage(StatementSyntax statement, int stageIndex,
            List<SceneReference> references, List<SceneDiagnostic> diagnostics) {
        if (statement instanceof ExpressionStatementSyntax expression) {
            Optional<SceneCommandBinder.BoundCommand> command = commandBinder.bind(expression.expression(),
                    step(stageIndex, 0), diagnostics);
            command.ifPresent(bound -> references.addAll(bound.references()));
            return command.map(bound -> new SceneStage(stageIndex, List.of(bound.command())));
        }
        if (statement instanceof SectionStatementSyntax section && parallel(section)) {
            return parallelStage(section, stageIndex, references, diagnostics);
        }
        diagnostics.add(error("SCENE_COMMAND_FAILED", "Only commands and Parallel blocks are valid in Scene.",
                statement.span()));
        return Optional.empty();
    }

    private Optional<SceneStage> parallelStage(SectionStatementSyntax section, int stageIndex,
            List<SceneReference> references, List<SceneDiagnostic> diagnostics) {
        List<SceneCommand> commands = new ArrayList<>();
        int branch = 0;
        for (StatementSyntax statement : section.body().statements()) {
            if (!(statement instanceof ExpressionStatementSyntax expression)) {
                diagnostics.add(error("SCENE_CHILD_FAILED", "Parallel children must be commands.", statement.span()));
                return Optional.empty();
            }
            Optional<SceneCommandBinder.BoundCommand> bound = commandBinder.bind(expression.expression(),
                    step(stageIndex, branch++), diagnostics);
            bound.ifPresent(value -> {
                commands.add(value.command());
                references.addAll(value.references());
            });
        }
        return commands.isEmpty() ? Optional.empty() : Optional.of(new SceneStage(stageIndex, commands));
    }

    private boolean parallel(SectionStatementSyntax section) {
        return section.invocation().target() instanceof io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax name
                && name.name().equals("Parallel") && section.invocation().arguments().isEmpty();
    }

    private SceneStepId step(int stage, int branch) {
        return new SceneStepId("step_" + stage + "_" + branch);
    }

    private BindingResult failed(List<SceneDiagnostic> diagnostics) {
        return new BindingResult(Optional.empty(), Optional.empty(), diagnostics);
    }

    private SceneDiagnostic error(String code, String message,
            io.github.kxyr0.echoscript.api.source.SourceSpan span) {
        return new SceneDiagnostic(SceneDiagnostic.Severity.ERROR, code, message, span);
    }
}
