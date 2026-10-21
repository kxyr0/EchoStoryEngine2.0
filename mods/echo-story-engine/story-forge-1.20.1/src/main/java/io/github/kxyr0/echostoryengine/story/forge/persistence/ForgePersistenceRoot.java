package io.github.kxyr0.echostoryengine.story.forge.persistence;

import io.github.kxyr0.echostoryengine.story.forge.EchoStoryEngineMod;
import java.nio.file.Path;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Привязка durable story storage к активному world directory.
 */
public final class ForgePersistenceRoot {
    private ForgePersistenceRoot() {
    }

    public static Path executionRoot(MinecraftServer server) {
        return Objects.requireNonNull(server, "server")
                .getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(EchoStoryEngineMod.MOD_ID)
                .resolve("executions");
    }
}
