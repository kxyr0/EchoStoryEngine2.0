package io.github.kxyr0.echoiteminspection.forge;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoiteminspection.catalog.InspectionCatalog;
import io.github.kxyr0.echoiteminspection.compile.InspectionSource;
import io.github.kxyr0.echoiteminspection.forge.resource.InspectionSources;
import io.github.kxyr0.echoiteminspection.forge.resource.InspectionItemCatalog;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import net.minecraft.server.packs.resources.ResourceManager;

/** Client host state; no client types in the catalog or discovery boundary. */
public final class InspectionRuntime {
    private static final Logger LOG = LogUtils.getLogger();
    private static final InspectionRuntime INSTANCE = new InspectionRuntime();
    private final InspectionCatalog catalog = new InspectionCatalog();
    private final InspectionSources sources = new InspectionSources(FMLPaths.GAMEDIR.get());
    private List<InspectionSource> previous;
    private String status = "Not loaded";

    private InspectionRuntime() { }

    public static InspectionCatalog catalog() { return INSTANCE.catalog; }
    public static String status() { return INSTANCE.status; }
    public static boolean reload() { return INSTANCE.load(true); }
    public static void poll() { INSTANCE.load(false); }

    public static void resourceReload(ResourceManager manager) {
        try {
            INSTANCE.sources.resourceReload(manager);
            INSTANCE.load(true);
        } catch (IOException | RuntimeException exception) {
            INSTANCE.status = "Resource read failed; LKG retained: " + exception.getMessage();
            LOG.warn("Echo Item Inspection: {}", INSTANCE.status);
        }
    }

    private boolean load(boolean force) {
        try {
            List<InspectionSource> candidate = sources.read();
            if (!force && candidate.equals(previous)) return false;
            previous = candidate;
            var knownItems = ForgeRegistries.ITEMS.getKeys().stream().map(Object::toString)
                    .collect(Collectors.toUnmodifiableSet());
            InspectionItemCatalog.export(knownItems);
            var result = catalog.reload(candidate, knownItems);
            status = "applied=" + result.applied() + ", diagnostics=" + result.diagnostics().size();
            result.diagnostics().forEach(diagnostic -> LOG.warn("{} {}: {} {}", diagnostic.code(),
                    diagnostic.span(), diagnostic.message(), diagnostic.suggestions().isEmpty()
                            ? "" : "Did you mean: " + diagnostic.suggestions()));
            LOG.info("Echo Item Inspection reload {}", status);
            return result.applied();
        } catch (IOException | RuntimeException exception) {
            String error = "Read failed; LKG retained: " + exception.getMessage();
            if (!error.equals(status)) LOG.warn("Echo Item Inspection: {}", error);
            status = error;
            return false;
        }
    }
}
