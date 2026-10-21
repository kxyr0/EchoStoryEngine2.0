package io.github.kxyr0.echostoryengine.blackspace.shader;

import java.util.Optional;

public final class BlackspaceShaderState {
    public static final String SHADERPACK_FILE_NAME = "EchoBlackspaceShader.zip";
    public static final String SHADERPACK_RESOURCE = "/bundled-shaderpacks/" + SHADERPACK_FILE_NAME;

    private BlackspaceShaderState() {
    }

    public record Snapshot(
            boolean shadersEnabled,
            Optional<String> configuredShaderPack,
            String currentPackName,
            ConfigFileSnapshot configFile
    ) {
        String packLabel() {
            if (currentPackName != null && !currentPackName.isBlank()) {
                return currentPackName;
            }
            return configuredShaderPack.orElse("off");
        }
    }

    public record ConfigFileSnapshot(boolean exists, byte[] bytes) {
    }
}
