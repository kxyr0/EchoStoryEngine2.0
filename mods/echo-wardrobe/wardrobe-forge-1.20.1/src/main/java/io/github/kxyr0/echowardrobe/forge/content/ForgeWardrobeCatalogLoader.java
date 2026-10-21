package io.github.kxyr0.echowardrobe.forge.content;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnostic;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.profile.WardrobeProfileDefinition;
import io.github.kxyr0.echowardrobe.api.revision.WardrobeCatalogRevision;
import io.github.kxyr0.echowardrobe.core.binding.WearableDefinitionCompiler;
import io.github.kxyr0.echowardrobe.core.catalog.OutfitVariantCatalog;
import io.github.kxyr0.echowardrobe.core.catalog.WardrobeCatalog;
import io.github.kxyr0.echowardrobe.core.catalog.WardrobeProfileCatalog;
import io.github.kxyr0.echowardrobe.core.catalog.WearableCatalog;
import io.github.kxyr0.echowardrobe.core.outfit.OutfitVariantCompiler;
import io.github.kxyr0.echowardrobe.core.profile.WardrobeProfileCompiler;
import io.github.kxyr0.echowardrobe.core.reload.WardrobeAssetFingerprint;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Loads W2 wardrobe scripts from external story content packs.
 */
public final class ForgeWardrobeCatalogLoader {
    private final Path gameDir;
    private final WardrobeProfileCompiler profileCompiler =
            new WardrobeProfileCompiler(EchoScriptLanguage.parser());
    private final WearableDefinitionCompiler wearableCompiler =
            new WearableDefinitionCompiler(EchoScriptLanguage.parser());
    private final OutfitVariantCompiler outfitCompiler =
            new OutfitVariantCompiler(EchoScriptLanguage.parser());
    private final ForgeItemRegistry itemRegistry = new ForgeItemRegistry();

    public ForgeWardrobeCatalogLoader(Path gameDir) {
        this.gameDir = gameDir;
    }

    public WardrobeResult<WardrobeCatalog> load() {
        List<WardrobeDiagnostic> diagnostics = new ArrayList<>();
        List<Path> roots = contentPackRoots(diagnostics);
        ExternalSkinAssets assets = new ExternalSkinAssets(roots, Set.of("echo_content_player:player/base"));
        List<SourceFile> profiles = sources(roots, "profiles", ".wardrobe-profile.es", diagnostics);
        List<SourceFile> wearables = sources(roots, "wearables", ".wearable.es", diagnostics);
        List<SourceFile> outfits = sources(roots, "outfits", ".outfit.es", diagnostics);
        List<WardrobeProfileDefinition> profileDefinitions = compileProfiles(profiles, diagnostics);
        List<WearableDefinition> wearableDefinitions = compileWearables(wearables, diagnostics);
        List<OutfitVariantDefinition> outfitDefinitions = compileOutfits(outfits, assets, diagnostics);
        if (diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        long revision = revision(profiles, wearables, outfits);
        WardrobeResult<WearableCatalog> wearableCatalog = WearableCatalog.of(wearableDefinitions, revision);
        WardrobeResult<OutfitVariantCatalog> outfitCatalog = OutfitVariantCatalog.of(outfitDefinitions);
        diagnostics.addAll(wearableCatalog.diagnostics());
        diagnostics.addAll(outfitCatalog.diagnostics());
        if (!wearableCatalog.successful() || !outfitCatalog.successful()
                || diagnostics.stream().anyMatch(WardrobeDiagnostic::error)) {
            return WardrobeResult.invalid(diagnostics);
        }
        WardrobeCatalog catalog = new WardrobeCatalog(WardrobeProfileCatalog.of(profileDefinitions),
                wearableCatalog.value().orElseThrow(), outfitCatalog.value().orElseThrow(),
                new WardrobeCatalogRevision(revision));
        return WardrobeResult.valid(catalog, diagnostics);
    }

    private List<Path> contentPackRoots(List<WardrobeDiagnostic> diagnostics) {
        Path root = gameDir.resolve("story").resolve("content-pack");
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.list(root)) {
            return stream.filter(Files::isDirectory).sorted(Comparator.comparing(Path::toString)).toList();
        } catch (IOException exception) {
            diagnostics.add(WardrobeDiagnostic.error(WardrobeDiagnosticCode.WARDROBE_RELOAD_FAILED,
                    "Cannot list wardrobe content packs: " + exception.getMessage(), root.toString()));
            return List.of();
        }
    }

    private List<SourceFile> sources(List<Path> roots, String kind, String suffix,
            List<WardrobeDiagnostic> diagnostics) {
        List<SourceFile> result = new ArrayList<>();
        for (Path root : roots) {
            Path scriptRoot = root.resolve("echo_wardrobe").resolve(kind);
            if (!Files.isDirectory(scriptRoot)) {
                continue;
            }
            try (var stream = Files.walk(scriptRoot)) {
                stream.filter(path -> path.getFileName().toString().endsWith(suffix))
                        .sorted(Comparator.comparing(Path::toString))
                        .map(path -> read(path, diagnostics))
                        .flatMap(java.util.Optional::stream)
                        .forEach(result::add);
            } catch (IOException exception) {
                diagnostics.add(WardrobeDiagnostic.error(WardrobeDiagnosticCode.WARDROBE_RELOAD_FAILED,
                        "Cannot scan wardrobe " + kind + ": " + exception.getMessage(), scriptRoot.toString()));
            }
        }
        return List.copyOf(result);
    }

    private java.util.Optional<SourceFile> read(Path path, List<WardrobeDiagnostic> diagnostics) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            String text = Files.readString(path);
            String fingerprint = WardrobeAssetFingerprint.sha256(bytes).value();
            return java.util.Optional.of(new SourceFile(path, text, fingerprint));
        } catch (IOException exception) {
            diagnostics.add(WardrobeDiagnostic.error(WardrobeDiagnosticCode.WARDROBE_RELOAD_FAILED,
                    "Cannot read wardrobe source: " + exception.getMessage(), path.toString()));
            return java.util.Optional.empty();
        }
    }

    private List<WardrobeProfileDefinition> compileProfiles(List<SourceFile> sources,
            List<WardrobeDiagnostic> diagnostics) {
        List<WardrobeProfileDefinition> result = new ArrayList<>();
        for (SourceFile source : sources) {
            WardrobeResult<List<WardrobeProfileDefinition>> compiled = profileCompiler.compile(source.id(),
                    source.text(), source.fingerprint());
            diagnostics.addAll(compiled.diagnostics());
            compiled.value().ifPresent(result::addAll);
        }
        return List.copyOf(result);
    }

    private List<WearableDefinition> compileWearables(List<SourceFile> sources,
            List<WardrobeDiagnostic> diagnostics) {
        List<WearableDefinition> result = new ArrayList<>();
        for (SourceFile source : sources) {
            WardrobeResult<List<WearableDefinition>> compiled = wearableCompiler.compile(source.id(),
                    source.text(), source.fingerprint(), itemRegistry);
            diagnostics.addAll(compiled.diagnostics());
            compiled.value().ifPresent(result::addAll);
        }
        return List.copyOf(result);
    }

    private List<OutfitVariantDefinition> compileOutfits(List<SourceFile> sources, ExternalSkinAssets assets,
            List<WardrobeDiagnostic> diagnostics) {
        List<OutfitVariantDefinition> result = new ArrayList<>();
        for (SourceFile source : sources) {
            WardrobeResult<List<OutfitVariantDefinition>> compiled = outfitCompiler.compile(source.id(),
                    source.text(), source.fingerprint(), assets);
            diagnostics.addAll(compiled.diagnostics());
            compiled.value().ifPresent(result::addAll);
        }
        return List.copyOf(result);
    }

    private long revision(List<SourceFile> profiles, List<SourceFile> wearables, List<SourceFile> outfits) {
        long hash = 17L;
        for (SourceFile source : concat(profiles, wearables, outfits)) {
            hash = 31L * hash + source.path().toString().hashCode();
            hash = 31L * hash + source.fingerprint().hashCode();
        }
        return Math.max(1L, hash & Long.MAX_VALUE);
    }

    private List<SourceFile> concat(List<SourceFile> profiles, List<SourceFile> wearables,
            List<SourceFile> outfits) {
        List<SourceFile> result = new ArrayList<>(profiles);
        result.addAll(wearables);
        result.addAll(outfits);
        return result;
    }

    private record SourceFile(Path path, String text, String fingerprint) {
        String id() {
            return path.toString();
        }
    }
}
