package io.github.kxyr0.echoiteminspection.testmod.client.ipla;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

final class IplaSmokeEvidence {
    private final Map<String, Object> results = new LinkedHashMap<>();

    void check(String name, boolean result) {
        record(name, result);
        if (!result) throw new AssertionError(name);
    }

    void record(String name, Object value) {
        results.put(name, value);
        LogUtils.getLogger().info("IPLA_INSPECTION_SMOKE {}={}", name, value);
    }

    void capture(String name) {
        var minecraft = Minecraft.getInstance();
        Screenshot.grab(minecraft.gameDirectory, "ipla-inspection-" + name + ".png", minecraft.getMainRenderTarget(),
                message -> LogUtils.getLogger().info("IPLA_INSPECTION_SMOKE screenshot={}", message.getString()));
    }

    void finish(Throwable failure) {
        record("status", failure == null ? "PASS" : "FAIL");
        if (failure != null) {
            record("failure", failure.toString());
            LogUtils.getLogger().error("IPLA_INSPECTION_SMOKE_FAILED", failure);
        }
        try {
            Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("ipla-inspection-smoke.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(results));
        } catch (java.io.IOException exception) { throw new RuntimeException(exception); }
    }
}
