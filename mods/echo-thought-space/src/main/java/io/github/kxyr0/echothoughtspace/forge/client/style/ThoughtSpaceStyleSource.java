package io.github.kxyr0.echothoughtspace.forge.client.style;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echothoughtspace.core.combination.ThoughtCombinationCatalog;
import io.github.kxyr0.echothoughtspace.core.combination.ThoughtCombinationRecipe;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProvider;
import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import io.github.kxyr0.echothoughtspace.core.model.DeclaredThoughtProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class ThoughtSpaceStyleSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Duration SCAN_INTERVAL = Duration.ofMillis(350L);
    private final Path root;
    private final ThoughtSpaceConfigCompiler compiler = new ThoughtSpaceConfigCompiler();
    private ThoughtSpaceStyleConfig current = ThoughtSpaceStyleConfig.defaults();
    private ThoughtCombinationCatalog combinations = ThoughtCombinationCatalog.empty();
    private final DeclaredThoughtProvider declaredThoughts = new DeclaredThoughtProvider();
    private List<FileStamp> observed;
    private long nextScanAtMs;
    private long revision;
    private int fileCount;

    public ThoughtSpaceStyleSource() { this(defaultRoot()); }
    ThoughtSpaceStyleSource(Path root) { this.root = root.toAbsolutePath().normalize(); }

    public ThoughtSpaceStyleConfig current() { return current; }
    public ThoughtCombinationCatalog combinations() { return combinations; }
    public ThoughtNodeProvider declaredThoughts() { return declaredThoughts; }
    public long revision() { return revision; }
    public int fileCount() { return fileCount; }
    public Path root() { return root; }

    public void refresh() {
        long now = System.currentTimeMillis();
        if (now < nextScanAtMs) return;
        nextScanAtMs = now + SCAN_INTERVAL.toMillis();
        reloadChanged();
    }

    void reloadNow() {
        nextScanAtMs = 0L;
        reloadChanged();
    }

    private void reloadChanged() {
        try {
            List<FileStamp> stamps = stamps();
            if (stamps.equals(observed)) return;
            observed = stamps;
            LoadedConfig loaded = load(stamps);
            declaredThoughts.replace(loaded.thoughts());
            current = loaded.style();
            combinations = loaded.combinations();
            fileCount = stamps.size();
            revision++;
            LOGGER.info("Thought Space config reloaded: {} file(s), {} thought(s), {} combination(s), revision {}.",
                    fileCount, declaredThoughts.nodes().size(), combinations.size(), revision);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Thought Space style rejected; keeping revision {}: {}", revision, exception.toString());
        }
    }

    private LoadedConfig load(List<FileStamp> stamps) throws IOException {
        ThoughtSpaceStyleConfig style = ThoughtSpaceStyleConfig.defaults();
        List<ThoughtCombinationRecipe> recipes = new ArrayList<>();
        List<ThoughtNodeSnapshot> thoughts = new ArrayList<>();
        for (FileStamp stamp : stamps) {
            String source = Files.readString(stamp.path(), StandardCharsets.UTF_8);
            ScriptIdentity identity = identity(stamp.path());
            var compiled = compiler.compile(identity.sourceId(), source, style,
                    identity.namespace(), identity.resourcePath());
            if (compiled.style().isPresent()) style = compiled.style().get();
            recipes.addAll(compiled.recipes());
            thoughts.addAll(compiled.thoughts());
        }
        return new LoadedConfig(style, new ThoughtCombinationCatalog(recipes), thoughts);
    }

    private ScriptIdentity identity(Path file) {
        Path relative = root.relativize(file);
        if (relative.getNameCount() < 3 || !relative.getName(1).toString().equals("echo_thought_space")) {
            throw new IllegalArgumentException("Thought Space config path must be <namespace>/echo_thought_space/*.es");
        }
        String namespace = relative.getName(0).toString();
        StringBuilder resource = new StringBuilder();
        for (int index = 2; index < relative.getNameCount(); index++) {
            if (!resource.isEmpty()) resource.append('/');
            resource.append(relative.getName(index));
        }
        String suffix = ".thought-space.es";
        String resourcePath = resource.substring(0, resource.length() - suffix.length());
        if (!namespace.matches("[a-z0-9_.-]+") || !resourcePath.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Thought Space config identity must be lowercase: " + relative);
        }
        return new ScriptIdentity(relative.toString().replace('\\', '/'), namespace, resourcePath);
    }

    private List<FileStamp> stamps() throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        List<Path> paths;
        try (Stream<Path> stream = Files.find(root, 8, (path, attributes) ->
                attributes.isRegularFile() && path.getFileName().toString().endsWith(".thought-space.es"))) {
            paths = stream.sorted(Comparator.naturalOrder()).toList();
        }
        List<FileStamp> result = new ArrayList<>(paths.size());
        for (Path path : paths) {
            result.add(new FileStamp(path, Files.getLastModifiedTime(path).toMillis(), Files.size(path)));
        }
        return List.copyOf(result);
    }

    private static Path defaultRoot() {
        try {
            return FMLPaths.GAMEDIR.get().resolve("story").resolve("configs");
        } catch (RuntimeException | LinkageError exception) {
            return Path.of("run").resolve("story").resolve("configs");
        }
    }

    private record FileStamp(Path path, long modifiedAtMs, long size) { }
    private record ScriptIdentity(String sourceId, String namespace, String resourcePath) { }
    private record LoadedConfig(ThoughtSpaceStyleConfig style, ThoughtCombinationCatalog combinations,
                                List<ThoughtNodeSnapshot> thoughts) { }
}
