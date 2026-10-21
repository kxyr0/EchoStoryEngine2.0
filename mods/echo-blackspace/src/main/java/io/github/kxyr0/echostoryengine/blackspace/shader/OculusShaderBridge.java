package io.github.kxyr0.echostoryengine.blackspace.shader;

import io.github.kxyr0.echostoryengine.blackspace.EchoBlackspace;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.config.IrisConfig;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

public final class OculusShaderBridge {
    private static final String OCULUS_MOD_ID = "oculus";
    private static final Path OCULUS_CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("oculus.properties");
    private static final Path RECOVERY_MARKER_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("echo_blackspace_shader_recovery.properties");
    private static final String MARKER_ACTIVE = "active";
    private static final String MARKER_SHADERS_ENABLED = "shadersEnabled";
    private static final String MARKER_SHADER_PACK = "shaderPack";
    private static BlackspaceShaderState.Snapshot snapshot;
    private static boolean blackspaceShaderActive;

    private OculusShaderBridge() {
    }

    public static boolean isOculusLoaded() {
        return ModList.get().isLoaded(OCULUS_MOD_ID);
    }

    public static boolean enterBlackspace() {
        if (!isOculusLoaded() || blackspaceShaderActive) {
            return blackspaceShaderActive;
        }
        BlackspaceShaderState.Snapshot currentSnapshot;
        try {
            currentSnapshot = snapshot();
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Cannot snapshot Oculus config before Blackspace shader switch.", exception);
            return false;
        }

        snapshot = currentSnapshot;
        try {
            writeRecoveryMarker(currentSnapshot);
            IrisConfig config = Iris.getIrisConfig();
            config.setShadersEnabled(true);
            config.setShaderPackName(BlackspaceShaderState.SHADERPACK_FILE_NAME);
            config.save();
            Iris.reload();
            blackspaceShaderActive = isBlackspacePipelineReady();
            if (blackspaceShaderActive) {
                EchoBlackspace.LOGGER.info("Blackspace switched Oculus shaderpack from '{}' to '{}'.",
                        currentSnapshot.packLabel(), BlackspaceShaderState.SHADERPACK_FILE_NAME);
            }
            return blackspaceShaderActive;
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Failed to switch Oculus to Blackspace shaderpack.", exception);
            snapshot = null;
            return false;
        } finally {
            restoreUserConfig(currentSnapshot);
        }
    }

    public static void restorePrevious() {
        BlackspaceShaderState.Snapshot currentSnapshot = snapshot;
        if (currentSnapshot == null) {
            recoverMarkedShaderIfNeeded();
            blackspaceShaderActive = false;
            return;
        }
        try {
            applySnapshotForReload(currentSnapshot);
            Iris.reload();
            deleteRecoveryMarker();
            snapshot = null;
            EchoBlackspace.LOGGER.info("Blackspace restored Oculus shader state '{}'.",
                    currentSnapshot.packLabel());
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Failed to restore Oculus shader state '{}'.",
                    currentSnapshot.packLabel(), exception);
        } finally {
            blackspaceShaderActive = false;
            restoreUserConfig(currentSnapshot);
        }
    }

    public static void restoreImmediately() {
        BlackspaceShaderState.Snapshot currentSnapshot = snapshot;
        if (currentSnapshot == null) {
            recoverMarkedShaderIfNeeded();
            blackspaceShaderActive = false;
            return;
        }
        try {
            applySnapshotForReload(currentSnapshot);
            Iris.reload();
            deleteRecoveryMarker();
            snapshot = null;
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Failed to immediately restore Oculus shader state '{}'.",
                    currentSnapshot.packLabel(), exception);
        } finally {
            blackspaceShaderActive = false;
            restoreUserConfig(currentSnapshot);
        }
    }

    public static boolean isBlackspacePipelineReady() {
        return isOculusLoaded()
                && IrisApi.getInstance().isShaderPackInUse()
                && BlackspaceShaderState.SHADERPACK_FILE_NAME.equals(Iris.getCurrentPackName());
    }

    public static boolean isBlackspaceShaderActive() {
        return blackspaceShaderActive && isBlackspacePipelineReady();
    }

    public static boolean recoverMarkedShaderIfNeeded() {
        if (!isOculusLoaded() || !Files.exists(RECOVERY_MARKER_PATH)) {
            return false;
        }
        Optional<RecoveryMarker> marker = readRecoveryMarker();
        if (marker.isEmpty()) {
            return false;
        }
        if (!isBlackspaceSelected()) {
            deleteRecoveryMarker();
            return false;
        }
        try {
            applyMarkerForReload(marker.orElseThrow());
            Iris.reload();
            deleteRecoveryMarker();
            blackspaceShaderActive = false;
            EchoBlackspace.LOGGER.info("Recovered Oculus shader state '{}' from Blackspace marker.",
                    marker.orElseThrow().packLabel());
            return true;
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Failed to recover Oculus shader state '{}' from Blackspace marker.",
                    marker.orElseThrow().packLabel(), exception);
            return false;
        }
    }

    private static BlackspaceShaderState.Snapshot snapshot() throws IOException {
        IrisConfig config = Iris.getIrisConfig();
        return new BlackspaceShaderState.Snapshot(
                config.areShadersEnabled(),
                config.getShaderPackName(),
                Iris.getCurrentPackName(),
                captureConfigFile()
        );
    }

    private static BlackspaceShaderState.ConfigFileSnapshot captureConfigFile() throws IOException {
        if (!Files.exists(OCULUS_CONFIG_PATH)) {
            return new BlackspaceShaderState.ConfigFileSnapshot(false, new byte[0]);
        }
        return new BlackspaceShaderState.ConfigFileSnapshot(true, Files.readAllBytes(OCULUS_CONFIG_PATH));
    }

    private static void applySnapshotForReload(BlackspaceShaderState.Snapshot currentSnapshot) throws IOException {
        IrisConfig config = Iris.getIrisConfig();
        config.setShadersEnabled(currentSnapshot.shadersEnabled());
        config.setShaderPackName(currentSnapshot.configuredShaderPack().orElse(null));
        config.save();
    }

    private static void applyMarkerForReload(RecoveryMarker marker) throws IOException {
        IrisConfig config = Iris.getIrisConfig();
        config.setShadersEnabled(marker.shadersEnabled());
        config.setShaderPackName(marker.configuredShaderPack().orElse(null));
        config.save();
    }

    private static void writeRecoveryMarker(BlackspaceShaderState.Snapshot currentSnapshot) throws IOException {
        Path parent = RECOVERY_MARKER_PATH.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties properties = new Properties();
        properties.setProperty(MARKER_ACTIVE, Boolean.toString(true));
        properties.setProperty(MARKER_SHADERS_ENABLED, Boolean.toString(currentSnapshot.shadersEnabled()));
        properties.setProperty(MARKER_SHADER_PACK, currentSnapshot.configuredShaderPack().orElse(""));
        try (OutputStream output = Files.newOutputStream(RECOVERY_MARKER_PATH)) {
            properties.store(output, "Echo Blackspace shader recovery marker");
        }
    }

    private static Optional<RecoveryMarker> readRecoveryMarker() {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(RECOVERY_MARKER_PATH)) {
            properties.load(input);
            if (!Boolean.parseBoolean(properties.getProperty(MARKER_ACTIVE, "false"))) {
                return Optional.empty();
            }
            boolean shadersEnabled = Boolean.parseBoolean(properties.getProperty(MARKER_SHADERS_ENABLED, "false"));
            String pack = properties.getProperty(MARKER_SHADER_PACK, "");
            return Optional.of(new RecoveryMarker(shadersEnabled, pack.isBlank() ? Optional.empty()
                    : Optional.of(pack)));
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Failed to read Blackspace shader recovery marker.", exception);
            return Optional.empty();
        }
    }

    private static void deleteRecoveryMarker() {
        try {
            Files.deleteIfExists(RECOVERY_MARKER_PATH);
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Failed to delete Blackspace shader recovery marker.", exception);
        }
    }

    private static boolean isBlackspaceSelected() {
        IrisConfig config = Iris.getIrisConfig();
        return BlackspaceShaderState.SHADERPACK_FILE_NAME.equals(Iris.getCurrentPackName())
                || BlackspaceShaderState.SHADERPACK_FILE_NAME.equals(config.getShaderPackName());
    }

    private static void restoreUserConfig(BlackspaceShaderState.Snapshot currentSnapshot) {
        try {
            BlackspaceShaderState.ConfigFileSnapshot configFile = currentSnapshot.configFile();
            if (configFile.exists()) {
                Path parent = OCULUS_CONFIG_PATH.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(OCULUS_CONFIG_PATH, configFile.bytes());
            } else {
                Files.deleteIfExists(OCULUS_CONFIG_PATH);
            }
        } catch (IOException exception) {
            EchoBlackspace.LOGGER.warn("Failed to restore Oculus config file after Blackspace shader reload.",
                    exception);
        }

        IrisConfig config = Iris.getIrisConfig();
        config.setShadersEnabled(currentSnapshot.shadersEnabled());
        config.setShaderPackName(currentSnapshot.configuredShaderPack().orElse(null));
    }

    private record RecoveryMarker(boolean shadersEnabled, Optional<String> configuredShaderPack) {
        String packLabel() {
            return configuredShaderPack.orElse("off");
        }
    }
}
