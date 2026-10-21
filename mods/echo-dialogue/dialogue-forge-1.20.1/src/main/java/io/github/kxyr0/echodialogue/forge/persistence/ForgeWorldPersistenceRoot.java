package io.github.kxyr0.echodialogue.forge.persistence;

import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Resolves the world-local Echo Dialogue session storage root.
 */
public final class ForgeWorldPersistenceRoot {
    private ForgeWorldPersistenceRoot() {
    }

    public static Path sessionsRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("echo_dialogue")
                .resolve("sessions");
    }
}
