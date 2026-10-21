package io.github.kxyr0.echocore.client;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinecraftWorldValidatorTest {
    @TempDir Path world;
    @Test void validatesMinecraftNbtAndRejectsInvalidOrMissingData() throws Exception {
        var validator = new MinecraftWorldValidator();
        assertThrows(java.io.IOException.class, () -> validator.validate(world));
        CompoundTag root = new CompoundTag();
        CompoundTag data = new CompoundTag();
        data.putString("LevelName", "Test chapter");
        data.putInt("DataVersion", 3465);
        root.put("Data", data);
        NbtIo.writeCompressed(root, world.resolve("level.dat").toFile());
        validator.validate(world);
        validator.validate(world);
        Files.writeString(world.resolve("level.dat"), "not compressed Minecraft NBT");
        assertThrows(java.io.IOException.class, () -> validator.validate(world));
        NbtIo.writeCompressed(new CompoundTag(), world.resolve("level.dat").toFile());
        assertThrows(java.io.IOException.class, () -> validator.validate(world));
    }
}
