package io.github.kxyr0.echosavemigration.compile;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echosavemigration.api.MigrationId;
import io.github.kxyr0.echosavemigration.api.NodeId;
import io.github.kxyr0.echosavemigration.api.ResourceId;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import io.github.kxyr0.echosavemigration.definition.MigrationOperation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SaveMigrationCompiler {
    private final MigrationCallReader reader = new MigrationCallReader();

    public MigrationCompileResult compile(MigrationSourceInput input) {
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId(input.sourcePath()), input.sourceText()));
        List<MigrationDiagnostic> diagnostics = new ArrayList<>(parsed.diagnostics().stream()
                .map(value -> new MigrationDiagnostic(value.code(), value.message(), value.span().orElseThrow()))
                .toList());
        if (!parsed.successful()) {
            if (!diagnostics.isEmpty()) diagnostics.add(new MigrationDiagnostic("SAVE_MIGRATION_STEP_FAILED",
                    "Migration source is not a pure declarative resource.", diagnostics.get(0).span()));
            return new MigrationCompileResult(Optional.empty(), diagnostics);
        }
        return bind(parsed.syntaxTree().orElseThrow().classDeclaration(), input, new ArrayList<>(diagnostics));
    }

    private MigrationCompileResult bind(ClassSyntax resource, MigrationSourceInput input,
            List<MigrationDiagnostic> diagnostics) {
        if (!resource.baseType().name().text().equals("SaveMigrationResource")) {
            diagnostics.add(new MigrationDiagnostic("SAVE_MIGRATION_RESOURCE_NOT_FOUND",
                    "Expected SaveMigrationResource profile.", resource.span()));
            return failed(diagnostics);
        }
        List<MigrationCallReader.Call> calls = new ArrayList<>();
        for (MemberSyntax member : resource.members()) {
            if (member instanceof ClassCallSyntax call && reader.read(call).isPresent()) {
                calls.add(reader.read(call).orElseThrow());
            } else {
                diagnostics.add(new MigrationDiagnostic("SAVE_MIGRATION_STEP_FAILED",
                        "Only pure migration operations are allowed.", member.span()));
            }
        }
        if (!diagnostics.isEmpty()) return failed(diagnostics);
        Optional<MigrationCallReader.Call> from = unique(calls, "From", diagnostics);
        Optional<MigrationCallReader.Call> to = unique(calls, "To", diagnostics);
        if (from.isEmpty() || to.isEmpty()) return failed(diagnostics);
        try {
            List<MigrationOperation> operations = calls.stream()
                    .filter(call -> !call.name().equals("From") && !call.name().equals("To"))
                    .map(this::operation).toList();
            MigrationDefinition definition = new MigrationDefinition(id(input),
                    new SaveSchemaVersion(reader.integer(from.orElseThrow(), 0)),
                    new SaveSchemaVersion(reader.integer(to.orElseThrow(), 0)), input.revision(), operations);
            return new MigrationCompileResult(Optional.of(definition), diagnostics);
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            diagnostics.add(new MigrationDiagnostic("SAVE_MIGRATION_STEP_FAILED", exception.getMessage(), resource.span()));
            return failed(diagnostics);
        }
    }

    private MigrationOperation operation(MigrationCallReader.Call call) {
        return switch (call.name()) {
            case "RenameResource" -> new MigrationOperation.RenameResource(resource(call, 0), resource(call, 1));
            case "RenameDialogueNode" -> new MigrationOperation.RenameDialogueNode(resource(call, 0),
                    node(call, 1), node(call, 2));
            case "RenameSceneNode" -> new MigrationOperation.RenameSceneNode(resource(call, 0),
                    node(call, 1), node(call, 2));
            case "RenameState" -> new MigrationOperation.RenameState(resource(call, 0), resource(call, 1));
            case "RemoveState" -> new MigrationOperation.RemoveState(resource(call, 0));
            case "CopyState" -> new MigrationOperation.CopyState(resource(call, 0), resource(call, 1));
            case "MoveState" -> new MigrationOperation.MoveState(resource(call, 0), resource(call, 1));
            case "SetDefaultIfMissing" -> new MigrationOperation.SetDefaultIfMissing(resource(call, 0),
                    reader.state(call, 1));
            default -> throw new IllegalArgumentException("Forbidden migration operation: " + call.name());
        };
    }

    private Optional<MigrationCallReader.Call> unique(List<MigrationCallReader.Call> calls, String name,
            List<MigrationDiagnostic> diagnostics) {
        List<MigrationCallReader.Call> matches = calls.stream().filter(call -> call.name().equals(name)).toList();
        if (matches.size() != 1) {
            var span = calls.isEmpty() ? null : calls.get(0).span();
            if (span != null) diagnostics.add(new MigrationDiagnostic("SAVE_MIGRATION_STEP_FAILED",
                    "Expected exactly one " + name + " declaration.", span));
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private ResourceId resource(MigrationCallReader.Call call, int index) {
        return new ResourceId(reader.string(call, index));
    }

    private NodeId node(MigrationCallReader.Call call, int index) {
        return new NodeId(reader.string(call, index));
    }

    private MigrationId id(MigrationSourceInput input) {
        String file = input.sourcePath().replace('\\', '/');
        file = file.substring(file.lastIndexOf('/') + 1).replaceFirst("(?i)(\\.migration)?\\.es$", "");
        file = file.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("[^A-Za-z0-9_]+", "_")
                .toLowerCase(Locale.ROOT);
        return new MigrationId(input.contentNamespace() + ":" + file);
    }

    private MigrationCompileResult failed(List<MigrationDiagnostic> diagnostics) {
        return new MigrationCompileResult(Optional.empty(), diagnostics);
    }
}
