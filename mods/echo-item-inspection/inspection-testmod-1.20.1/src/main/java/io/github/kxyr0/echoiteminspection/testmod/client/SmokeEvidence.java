package io.github.kxyr0.echoiteminspection.testmod.client;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

public final class SmokeEvidence {
    private final Map<String, Object> results = new LinkedHashMap<>();
    private final List<String> screenshots = new ArrayList<>();

    void begin() throws java.io.IOException {
        Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots")
                .toAbsolutePath().normalize();
        Files.createDirectories(directory);
        try (var previous = Files.newDirectoryStream(directory, "inspection-smoke-*.png")) {
            for (Path path : previous) {
                if (!path.toAbsolutePath().normalize().getParent().equals(directory)) {
                    throw new java.io.IOException("Unexpected screenshot path: " + path);
                }
                Files.delete(path);
            }
        }
        record("startedAt", java.time.Instant.now().toString());
    }

    public void record(String key, Object value) {
        results.put(key, value);
        LogUtils.getLogger().info("ITEM_INSPECTION_SMOKE {}={}", key, value);
    }

    public void check(String key, boolean value) {
        record(key, value);
        if (!value) throw new AssertionError(key);
    }

    public void capture(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        String filename = "inspection-smoke-" + name + ".png";
        screenshots.add(filename);
        Screenshot.grab(minecraft.gameDirectory, filename, minecraft.getMainRenderTarget(),
                message -> LogUtils.getLogger().info("ITEM_INSPECTION_SMOKE screenshot {}: {}",
                        filename, message.getString()));
    }

    boolean capturesReady() {
        Minecraft minecraft = Minecraft.getInstance();
        return screenshots.stream().allMatch(name -> Files.isRegularFile(
                minecraft.gameDirectory.toPath().resolve("screenshots").resolve(name)));
    }

    void finish(Throwable failure) {
        Minecraft minecraft = Minecraft.getInstance();
        record("status", failure == null ? "PASS" : "FAIL");
        results.put("screenshots", List.copyOf(screenshots));
        if (failure != null) {
            results.put("failure", failure.toString());
            LogUtils.getLogger().error("ITEM_INSPECTION_LIVE_SMOKE_FAILED", failure);
        }
        Path path = minecraft.gameDirectory.toPath().resolve("inspection-smoke.json");
        try {
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(results));
        } catch (java.io.IOException exception) {
            LogUtils.getLogger().error("Cannot write live smoke evidence", exception);
        }
        LogUtils.getLogger().info(failure == null ? "ITEM_INSPECTION_LIVE_SMOKE_PASSED"
                : "ITEM_INSPECTION_LIVE_SMOKE_FAILED");
    }
}
