package io.github.kxyr0.echosavemigration.forge;

import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.compile.MigrationSourceInput;
import io.github.kxyr0.echosavemigration.compile.SaveMigrationCompiler;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import io.github.kxyr0.echosavemigration.graph.MigrationGraph;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

final class ForgeMigrationCatalog {
    private final AtomicLong revision = new AtomicLong();
    private volatile io.github.kxyr0.echosavemigration.graph.AliasGraph aliases =
            new io.github.kxyr0.echosavemigration.graph.AliasGraph();
    private volatile MigrationGraph graph = new MigrationGraph(List.of());

    SimplePreparableReloadListener<List<MigrationDefinition>> listener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected List<MigrationDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
                return compile(manager);
            }

            @Override
            protected void apply(List<MigrationDefinition> values, ResourceManager manager,
                    ProfilerFiller profiler) {
                var replacement = new MigrationGraph(values);
                var replacementAliases = io.github.kxyr0.echosavemigration.graph.MigrationResourceAliases.from(values);
                graph = replacement;
                aliases = replacementAliases;
            }
        };
    }

    io.github.kxyr0.echosavemigration.api.ResourceId canonical(
            io.github.kxyr0.echosavemigration.api.ResourceId id) { return aliases.canonical(id); }

    MigrationGraph graph() {
        return graph;
    }

    SaveSchemaVersion currentSchema() {
        return graph.definitions().stream().map(MigrationDefinition::to).max(SaveSchemaVersion::compareTo)
                .orElseGet(() -> new SaveSchemaVersion(0));
    }

    private List<MigrationDefinition> compile(ResourceManager manager) {
        List<MigrationDefinition> definitions = new ArrayList<>();
        SaveMigrationCompiler compiler = new SaveMigrationCompiler();
        manager.listResources("echo_save_migration/migrations", id -> id.getPath().endsWith(".es"))
                .forEach((id, resource) -> {
                    try (var reader = resource.openAsReader()) {
                        String text = reader.lines().collect(java.util.stream.Collectors.joining("\n")) + "\n";
                        String source = "data/" + id.getNamespace() + "/" + id.getPath();
                        var result = compiler.compile(new MigrationSourceInput(id.getNamespace(), source, text,
                                revision.incrementAndGet()));
                        if (!result.successful()) {
                            throw new IllegalStateException("SAVE_MIGRATION_STEP_FAILED: " + id + " "
                                    + result.diagnostics());
                        }
                        definitions.add(result.definition().orElseThrow());
                    } catch (IOException exception) {
                        throw new IllegalStateException("SAVE_MIGRATION_RESOURCE_NOT_FOUND: " + id, exception);
                    }
                });
        return definitions;
    }
}
