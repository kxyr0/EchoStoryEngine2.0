package io.github.kxyr0.echoplayerhud.forge.config;

import io.github.kxyr0.echoplayerhud.api.result.HudDiagnostic;
import io.github.kxyr0.echoplayerhud.api.result.HudResult;
import io.github.kxyr0.echoplayerhud.core.config.HudEsLayoutParser;
import io.github.kxyr0.echoplayerhud.core.diagnostic.HudDiagnosticCodes;
import io.github.kxyr0.echoplayerhud.core.layout.HudLayoutSnapshot;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Loads the editable HUD .es config from the Forge config directory.
 */
public final class PlayerHudConfigLoader {
    private static final String RESOURCE = "/data/jdh/echo_player_hud/config/player_vitals_hud.es";
    private final Path configPath;
    private final HudEsLayoutParser parser = new HudEsLayoutParser();
    private final AtomicLong revision = new AtomicLong(1L);

    private PlayerHudConfigLoader(Path configPath) {
        this.configPath = configPath;
    }

    public static PlayerHudConfigLoader create() {
        return new PlayerHudConfigLoader(FMLPaths.CONFIGDIR.get()
                .resolve("echo_player_hud")
                .resolve("player_vitals_hud.es"));
    }

    public HudResult<HudLayoutSnapshot> load() {
        try {
            ensureExternalConfig();
            String text = Files.readString(configPath, StandardCharsets.UTF_8);
            return parser.parse(text, configPath.toString(), revision.incrementAndGet());
        } catch (IOException | RuntimeException exception) {
            return HudResult.invalid(List.of(HudDiagnostic.error(HudDiagnosticCodes.CONFIG_PARSE_ERROR,
                    "Cannot load HUD config: " + exception.getMessage(), configPath.toString())));
        }
    }

    public Path configPath() {
        return configPath;
    }

    public long currentStamp() {
        try {
            ensureExternalConfig();
            return Files.getLastModifiedTime(configPath).toMillis() ^ Files.size(configPath);
        } catch (IOException exception) {
            return Long.MIN_VALUE;
        }
    }

    private void ensureExternalConfig() throws IOException {
        if (Files.isRegularFile(configPath)) {
            return;
        }
        Files.createDirectories(configPath.getParent());
        try (InputStream input = PlayerHudConfigLoader.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                Files.writeString(configPath, fallbackConfig(), StandardCharsets.UTF_8);
            } else {
                Files.copy(input, configPath);
            }
        }
    }

    private String fallbackConfig() {
        return """
                namespace JDH.Configs.PlayerHud;

                class PlayerVitalsHudConfig : PlayerHudConfigResource
                {
                    private void Build()
                    {
                        PlayerHud(id: "jdh:player_vitals_hud", referenceWidth: 1920, referenceHeight: 1080);
                        Bar(id: "echo_player_hud:health_bar", source: "echo_player_vitals:health",
                            anchor: "BOTTOM_LEFT", x: 36, y: 126, width: 260, height: 18,
                            background: "#99000000", gradient: "0:#B23A48,0.66:#E85D75");
                    }
                }
                """;
    }
}
