package io.github.kxyr0.echomagic.forge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class MagicGestureHudConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.GAMEDIR.get().resolve("story")
            .resolve("configs").resolve("jdh").resolve("magic_gesture_hud.json");
    private static final State STATE = new State();

    public static Layout current() {
        return STATE.current();
    }

    public record Position(int x, int y) { }

    public record Layout(Anchor anchor, int x, int y, int width, int height,
                         double mouseSensitivity, boolean freezeCamera) {
        public Position position(int screenWidth, int screenHeight) {
            return anchor.position(screenWidth, screenHeight, width, height, x, y);
        }

        public double gestureScale() {
            double widthScale = width / 142.0D;
            double heightScale = height / 112.0D;
            return Math.max(4.0D, 64.0D * Math.min(widthScale, heightScale));
        }
    }

    public enum Anchor {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

        private Position position(int screenWidth, int screenHeight, int width, int height,
                                  int offsetX, int offsetY) {
            int baseX = switch (this) {
                case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0;
                case TOP_CENTER, CENTER, BOTTOM_CENTER -> (screenWidth - width) / 2;
                case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenWidth - width;
            };
            int baseY = switch (this) {
                case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
                case CENTER_LEFT, CENTER, CENTER_RIGHT -> (screenHeight - height) / 2;
                case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - height;
            };
            return new Position(baseX + offsetX, baseY + offsetY);
        }

        private static Anchor parse(String value) {
            if (value == null) return TOP_RIGHT;
            String normalized = value.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_').replace(' ', '_');
            normalized = switch (normalized) {
                case "TOPLEFT" -> "TOP_LEFT";
                case "TOP" -> "TOP_CENTER";
                case "TOPRIGHT" -> "TOP_RIGHT";
                case "LEFT" -> "CENTER_LEFT";
                case "RIGHT" -> "CENTER_RIGHT";
                case "BOTTOMLEFT" -> "BOTTOM_LEFT";
                case "BOTTOM" -> "BOTTOM_CENTER";
                case "BOTTOMRIGHT" -> "BOTTOM_RIGHT";
                default -> normalized;
            };
            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                LOGGER.warn("Unknown gesture HUD anchor '{}'; using TOP_RIGHT", value);
                return TOP_RIGHT;
            }
        }
    }

    private static Layout load() throws IOException {
        Files.createDirectories(FILE.getParent());
        if (!Files.exists(FILE)) writeDefaults();
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data == null) data = new Data();
            return new Layout(Anchor.parse(data.anchor), clamp(data.x, -32768, 32768),
                    clamp(data.y, -32768, 32768), clamp(data.width, 32, 4096),
                    clamp(data.height, 32, 4096), clamp(data.mouseSensitivity, 0.05D, 10.0D),
                    data.freezeCamera);
        }
    }

    private static void writeDefaults() throws IOException {
        try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(new Data(), writer);
        }
    }

    private static long modifiedTime() {
        try {
            return Files.exists(FILE) ? Files.getLastModifiedTime(FILE).toMillis() : -1L;
        } catch (IOException exception) {
            return -1L;
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) return 1.0D;
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static final class State {
        private Layout layout;
        private long lastModified;
        private long nextCheckMillis;

        private State() {
            try {
                layout = load();
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Failed to load gesture HUD config {}", FILE, exception);
                layout = defaults();
            }
            lastModified = modifiedTime();
        }

        private Layout current() {
            long now = System.currentTimeMillis();
            if (now < nextCheckMillis) return layout;
            nextCheckMillis = now + 500L;
            long modified = modifiedTime();
            if (modified == lastModified) return layout;
            try {
                layout = load();
                lastModified = modifiedTime();
            } catch (IOException | RuntimeException exception) {
                lastModified = modified;
                LOGGER.error("Failed to reload gesture HUD config {}; keeping previous values", FILE, exception);
            }
            return layout;
        }
    }

    private static Layout defaults() {
        Data data = new Data();
        return new Layout(Anchor.TOP_RIGHT, data.x, data.y, data.width, data.height,
                data.mouseSensitivity, data.freezeCamera);
    }

    private static final class Data {
        private String anchor = "TOP_RIGHT";
        private int x = -12;
        private int y = 26;
        private int width = 142;
        private int height = 112;
        @SerializedName("mouse_sensitivity")
        private double mouseSensitivity = 1.0D;
        @SerializedName("freeze_camera")
        private boolean freezeCamera = true;
    }

    private MagicGestureHudConfig() { }
}
