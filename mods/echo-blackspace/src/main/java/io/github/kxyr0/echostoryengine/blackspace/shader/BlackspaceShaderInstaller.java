package io.github.kxyr0.echostoryengine.blackspace.shader;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.client.Minecraft;

public final class BlackspaceShaderInstaller {
    private BlackspaceShaderInstaller() {
    }

    public static Optional<Path> installBundledShaderpack() {
        try (InputStream input = BlackspaceShaderInstaller.class.getResourceAsStream(
                BlackspaceShaderState.SHADERPACK_RESOURCE)) {
            if (input == null) {
                EchoBlackspace.LOGGER.warn("Bundled Blackspace shaderpack resource is missing: {}.",
                        BlackspaceShaderState.SHADERPACK_RESOURCE);
                return Optional.empty();
            }
            byte[] bundledBytes = input.readAllBytes();
            Path shaderpacks = Minecraft.getInstance().gameDirectory.toPath().resolve("shaderpacks");
            Path target = shaderpacks.resolve(BlackspaceShaderState.SHADERPACK_FILE_NAME);
            Files.createDirectories(shaderpacks);
            if (Files.exists(target) && Arrays.equals(Files.readAllBytes(target), bundledBytes)) {
                return Optional.of(target);
            }
            Files.write(target, bundledBytes);
            EchoBlackspace.LOGGER.info("Installed bundled Blackspace shaderpack to {}.", target);
            return Optional.of(target);
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Failed to install bundled Blackspace shaderpack.", exception);
            return Optional.empty();
        }
    }
}
