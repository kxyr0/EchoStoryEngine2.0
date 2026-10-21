package io.github.kxyr0.echostoryengine.story.forge.externalcontent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

/**
 * Publishes the external folder as a required Forge resource/data pack.
 */
public final class ExternalContentPackSource implements RepositorySource {
    private final Path packRoot;
    private final PackType packType;

    public ExternalContentPackSource(Path packRoot, PackType packType) {
        this.packRoot = packRoot.toAbsolutePath().normalize();
        this.packType = packType;
    }

    @Override
    public void loadPacks(Consumer<Pack> output) {
        if (!Files.isDirectory(packRoot) || Files.exists(packRoot.resolve(ExternalContentPaths.ARCHIVE_MARKER))) {
            return;
        }
        Pack pack = Pack.readMetaAndCreate(
                ExternalContentPaths.PACK_ID,
                Component.literal(ExternalContentPaths.PACK_TITLE),
                true,
                id -> new PathPackResources(id, packRoot, false),
                packType,
                Pack.Position.TOP,
                PackSource.WORLD);
        if (pack != null) {
            output.accept(pack);
        }
    }
}
