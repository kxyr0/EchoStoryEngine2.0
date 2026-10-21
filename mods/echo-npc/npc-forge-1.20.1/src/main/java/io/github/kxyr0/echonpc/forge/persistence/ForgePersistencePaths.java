package io.github.kxyr0.echonpc.forge.persistence;

import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Resolves world-scoped Echo NPC persistence roots through Minecraft world APIs.
 */
public final class ForgePersistencePaths {
    private ForgePersistencePaths() {
    }

    public static Path actorsRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("echo_npc").resolve("actors");
    }
}
