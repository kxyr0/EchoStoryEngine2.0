package io.github.kxyr0.echostoryengine.story.forge.externalcontent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires external content extraction and manual-reload pack registration.
 */
public final class ExternalContentLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger("io.github.kxyr0.echostoryengine.story");

    private final ExternalContentPaths paths;
    private final ExternalContentSeedExtractor extractor = new ExternalContentSeedExtractor();
    private boolean initialized;

    public ExternalContentLifecycle() {
        this(new ExternalContentPaths(FMLPaths.GAMEDIR.get()));
    }

    ExternalContentLifecycle(ExternalContentPaths paths) {
        this.paths = paths;
    }

    public void register(IEventBus modEventBus, IEventBus forgeEventBus) {
        modEventBus.addListener(this::onAddPackFinders);
        forgeEventBus.addListener(this::onServerStarted);
    }

    private void onAddPackFinders(AddPackFindersEvent event) {
        if (!supportsExternalContentPack(event.getPackType())) {
            return;
        }
        ensureInitialized();
        event.addRepositorySource(new ExternalContentPackSource(paths.packRoot(), event.getPackType()));
    }

    static boolean supportsExternalContentPack(PackType packType) {
        return packType == PackType.SERVER_DATA || packType == PackType.CLIENT_RESOURCES;
    }

    private void onServerStarted(ServerStartedEvent event) {
        ensureInitialized();
    }

    private synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        Path root = paths.packRoot();
        try {
            initializeSeed(root, ExternalContentPaths.SEED_ROOT, true);
            initializeSeed(paths.configRoot(), ExternalContentPaths.CONFIG_SEED_ROOT, false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("External story content initialization failed: {}", exception.toString());
        }
    }

    private void initializeSeed(Path root, String seedRoot, boolean required) throws IOException {
        IModFileInfo modFile = ModList.get().getModFileById(ExternalContentPaths.CONTENT_MOD_ID);
        if (modFile == null) {
            if (!Files.exists(root)) {
                LOGGER.warn("Embedded story content pack mod is not loaded: {}", ExternalContentPaths.CONTENT_MOD_ID);
            }
            LOGGER.info("External story content source: {}", root);
            return;
        }
        Path seed = modFile.getFile().findResource(seedRoot.split("/"));
        if (!Files.isDirectory(seed)) {
            if (required) {
                throw new IOException("Embedded story seed not found: " + seed);
            }
            return;
        }
        if (extractor.initialize(root, seed)) {
            LOGGER.info("External story content initialized or updated: {}", root);
        } else {
            LOGGER.info("External story content source: {}", root);
        }
    }
}
