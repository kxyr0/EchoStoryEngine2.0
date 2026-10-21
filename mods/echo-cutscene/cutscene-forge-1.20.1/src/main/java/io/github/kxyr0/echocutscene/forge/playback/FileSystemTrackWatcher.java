package io.github.kxyr0.echocutscene.forge.playback;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class FileSystemTrackWatcher {
    public Path root(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("echo").resolve("cutscenes");
    }

    public boolean exists(MinecraftServer server) {
        return Files.isDirectory(root(server));
    }
}
