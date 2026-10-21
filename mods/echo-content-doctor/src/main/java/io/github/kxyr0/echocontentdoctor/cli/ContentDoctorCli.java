package io.github.kxyr0.echocontentdoctor.cli;

import io.github.kxyr0.echocontentdoctor.engine.ContentDoctor;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctorInput;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctorResult;
import io.github.kxyr0.echocontentdoctor.provider.ContentCatalogAssembler;
import io.github.kxyr0.echocontentdoctor.provider.ContentSnapshotProvider;
import io.github.kxyr0.echocontentdoctor.provider.ItemSnapshotProvider;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.report.ContentDoctorConsole;
import io.github.kxyr0.echocontentdoctor.report.ContentDoctorJsonReport;
import io.github.kxyr0.echocontentdoctor.source.SourceCatalogScanner;
import io.github.kxyr0.echocontentdoctor.source.SourceScanResult;
import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.definition.MigrationDefinition;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class ContentDoctorCli {
    private ContentDoctorCli() { }

    public static void main(String[] args) {
        try {
            if (run(args) != 0) System.exit(1);
        } catch (Exception exception) {
            System.err.println("Content Doctor configuration error: " + exception.getMessage());
            System.exit(2);
        }
    }

    static int run(String[] args) throws Exception {
        boolean emptyAllowed = args.length == 2 && args[0].equals("--empty-catalog");
        boolean resourceMode = (args.length == 3 || args.length == 5 && args[2].equals("--item-catalog"))
                && args[0].equals("--resources");
        if (!emptyAllowed && !resourceMode && args.length != 1) {
            throw new IllegalArgumentException("Usage: --resources <roots> [--item-catalog <file>] <report> | --empty-catalog <report>");
        }
        Path report = Path.of(args[resourceMode ? args.length - 1 : emptyAllowed ? 1 : 0]);
        if (resourceMode) return validateSources(java.util.Arrays.stream(args[1].split(
                java.util.regex.Pattern.quote(File.pathSeparator))).map(Path::of).toList(), report,
                args.length == 5 ? Path.of(args[3]) : null);
        List<ContentSnapshotProvider> providers = ServiceLoader.load(ContentSnapshotProvider.class).stream()
                .map(ServiceLoader.Provider::get).toList();
        if (providers.isEmpty() && !emptyAllowed) {
            throw new IllegalStateException("No typed Echo content descriptor providers are installed.");
        }
        var catalog = new ContentCatalogAssembler().assemble(providers);
        var result = new ContentDoctor().validate(ContentDoctorInput.catalogOnly(catalog));
        new ContentDoctorJsonReport().write(report, result);
        System.out.print(new ContentDoctorConsole().format(result));
        return result.successful() ? 0 : 1;
    }

    private static int validateSources(List<Path> roots, Path report, Path itemCatalog) throws Exception {
        SourceCatalogScanner scanner = new SourceCatalogScanner();
        SourceScanResult scan = itemCatalog == null ? scanner.scan(roots)
                : scanner.scan(roots, ItemSnapshotProvider.read(itemCatalog));
        if (itemCatalog == null && scan.catalog().resources().stream()
                .anyMatch(value -> value.kind() == ContentResourceKind.ITEM_INSPECTION)) {
            throw new IllegalStateException("ITEM_INSPECTION_ITEM_CATALOG_REQUIRED: pass --item-catalog <registry-export.txt>.");
        }
        ContentDoctorInput input = input(scan);
        ContentDoctorResult semantic = new ContentDoctor().validate(input);
        var diagnostics = new ArrayList<>(scan.diagnostics());
        diagnostics.addAll(semantic.diagnostics());
        ContentDoctorResult result = new ContentDoctorResult(semantic.revision(), semantic.files(),
                semantic.resources(), semantic.references(), diagnostics);
        new ContentDoctorJsonReport().write(report, result);
        System.out.print(new ContentDoctorConsole().format(result));
        return result.successful() ? 0 : 1;
    }

    private static ContentDoctorInput input(SourceScanResult scan) {
        if (scan.migrations().isEmpty()) return ContentDoctorInput.catalogOnly(scan.catalog());
        SaveSchemaVersion minimum = scan.migrations().stream().map(MigrationDefinition::from)
                .min(SaveSchemaVersion::compareTo).orElseThrow();
        SaveSchemaVersion current = scan.migrations().stream().map(MigrationDefinition::to)
                .max(SaveSchemaVersion::compareTo).orElseThrow();
        return new ContentDoctorInput(scan.catalog(), minimum, current, scan.migrations());
    }
}
