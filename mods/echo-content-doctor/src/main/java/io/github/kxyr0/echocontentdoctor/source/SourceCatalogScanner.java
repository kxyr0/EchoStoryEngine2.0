package io.github.kxyr0.echocontentdoctor.source;

import io.github.kxyr0.echocontentdoctor.graph.ContentCatalogSnapshot;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.provider.ContentCatalogAssembler;
import io.github.kxyr0.echocontentdoctor.provider.ItemSnapshotProvider;
import io.github.kxyr0.echocontentdoctor.source.adapter.AnimationSourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.DeclarationSourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.DialogueSourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.migration.MigrationSourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.NpcSourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.SceneSourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.ScenarioSourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.StorySourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.inspection.InspectionSourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.adapter.item.ItemDefinitionSourceAdapter;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SourceCatalogScanner {
    public SourceScanResult scan(Path root) throws IOException {
        return scan(List.of(root));
    }

    public SourceScanResult scan(Path root, ItemSnapshotProvider items) throws IOException {
        return scan(List.of(root), items);
    }

    /** Registry keys are authoritative; a definition pending registration cannot satisfy an item reference. */
    public SourceScanResult scan(List<Path> roots, ItemSnapshotProvider items) throws IOException {
        SourceScanResult source = scan(roots);
        return new SourceScanResult(new ContentCatalogAssembler().withItems(source.catalog(), items),
                source.diagnostics(), source.migrations());
    }

    public SourceScanResult scan(List<Path> roots) throws IOException {
        List<Path> checkedRoots = roots.stream().map(value -> value.toAbsolutePath().normalize()).toList();
        checkedRoots.stream().filter(value -> !Files.isDirectory(value)).findFirst().ifPresent(value -> {
            throw new IllegalArgumentException("Content root is not a directory: " + value);
        });
        List<SourceFile> all = new ArrayList<>();
        for (Path root : checkedRoots) {
            try (var stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile).forEach(path -> all.add(new SourceFile(root, path)));
            }
        }
        List<Path> paths = all.stream().map(SourceFile::path).toList();
        List<SourceAdapter> adapters = adapters(paths);
        List<SourceFile> files = all.stream().filter(file -> accepted(file.path(), adapters))
                .sorted(Comparator.comparing(file -> file.path().toString())).toList();
        List<ContentDescriptor> descriptors = new ArrayList<>();
        List<MigrationDefinition> migrations = new ArrayList<>();
        var diagnostics = new ArrayList<io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic>();
        for (SourceFile file : files) {
            String sourceId = file.root().relativize(file.path()).toString().replace('\\', '/');
            String text = Files.readString(file.path(), StandardCharsets.UTF_8);
            SourceAdapter adapter = adapters.stream().filter(value -> value.accepts(file.path()))
                    .findFirst().orElseThrow();
            SourceAdapter.AdaptedSource adapted = new
                    io.github.kxyr0.echocontentdoctor.source.adapter.events.InlineEventSourceAdapter()
                    .enrich(sourceId, text, adapter.adapt(sourceId, text));
            descriptors.addAll(adapted.descriptors());
            migrations.addAll(adapted.migrations());
            adapted.problems().stream().map(ContentSourceProblem::diagnostic).forEach(diagnostics::add);
        }
        new io.github.kxyr0.echocontentdoctor.provider.audio.AudioDescriptorProvider()
                .append(checkedRoots, descriptors, diagnostics);
        if (descriptors.stream().flatMap(value -> value.eventSubscriptions().stream())
                .anyMatch(value -> value.subscription().filter().subject().isPresent())) {
            SourceSymbols.scenes(paths).symbols().stream()
                    .filter(symbol -> symbol.kind() == io.github.kxyr0.echoscene.compile.SceneSymbolKind.PLAYER)
                    .forEach(symbol -> {
                        var id = new io.github.kxyr0.echocontentdoctor.model.ContentResourceId(symbol.resourceId());
                        var kind = io.github.kxyr0.echocontentdoctor.model.ContentResourceKind.PLAYER;
                        if (descriptors.stream().noneMatch(value -> value.kind() == kind && value.id().equals(id))) {
                            descriptors.add(ContentDescriptor.simple(id, kind,
                                    SourceSpans.file("builtin:scene-player-catalog", ""), List.of()));
                        }
                    });
        }
        descriptors.sort(Comparator.comparing(value -> value.id().value()));
        long revision = descriptors.stream().mapToLong(value -> value.hashCode() & 0xffffffffL).sum();
        return new SourceScanResult(new ContentCatalogSnapshot(revision, files.size(), descriptors), diagnostics,
                migrations);
    }

    private List<SourceAdapter> adapters(List<Path> files) {
        return List.of(new io.github.kxyr0.echocontentdoctor.source.adapter.events.CutsceneSourceAdapter(),
                new io.github.kxyr0.echocontentdoctor.source.adapter.events.EventTriggerSourceAdapter(),
                new io.github.kxyr0.echocontentdoctor.source.adapter.events.ObservationSourceAdapter(SourceSymbols.scenes(files)),
                new StorySourceAdapter(SourceSymbols.scenes(files)), new DialogueSourceAdapter(), new NpcSourceAdapter(),
                new AnimationSourceAdapter(), new SceneSourceAdapter(SourceSymbols.scenes(files)),
                new DeclarationSourceAdapter(), new ScenarioSourceAdapter(SourceSymbols.scenarios(files)),
                new MigrationSourceAdapter(), new InspectionSourceAdapter(), new ItemDefinitionSourceAdapter(),
                new io.github.kxyr0.echocontentdoctor.source.adapter.audio.AudioActionSourceAdapter(SourceSymbols.scenes(files)));
    }

    private boolean accepted(Path path, List<SourceAdapter> adapters) {
        return adapters.stream().anyMatch(value -> value.accepts(path));
    }

    private record SourceFile(Path root, Path path) { }
}
