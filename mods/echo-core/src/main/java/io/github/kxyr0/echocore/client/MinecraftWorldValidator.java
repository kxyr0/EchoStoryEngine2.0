package io.github.kxyr0.echocore.client;

import io.github.kxyr0.echocore.core.ChapterFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

public final class MinecraftWorldValidator {
    private final Map<Path, Stamp> validated = new ConcurrentHashMap<>();
    public void validate(Path world) throws IOException {
        ChapterFiles.ordinary(world);
        Path level = world.resolve("level.dat");
        if (!Files.isRegularFile(level)) throw new IOException("Missing level.dat: " + world);
        ChapterFiles.ordinary(level);
        var stamp = new Stamp(Files.size(level), Files.getLastModifiedTime(level).toMillis());
        if (stamp.equals(validated.get(level))) return;
        CompoundTag data = NbtIo.readCompressed(level.toFile());
        if (!data.contains("Data", Tag.TAG_COMPOUND) || data.getCompound("Data").isEmpty()) {
            throw new IOException("level.dat does not contain Minecraft world data: " + world);
        }
        validated.put(level, stamp);
    }
    private record Stamp(long size, long modified) { }
}
