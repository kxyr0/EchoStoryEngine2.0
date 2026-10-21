package io.github.kxyr0.echoplayerfollow.client;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;

final class ClientModeGuards {
    private static final String DIALOGUE_CLIENT =
            "io.github.kxyr0.echodialogue.forge.client.ClientDialogueClient";
    private static final String CUTSCENE_CLIENT =
            "io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient";

    private ClientModeGuards() {
    }

    static boolean blocksActiveFollow(Minecraft minecraft) {
        return !activeBlockReason(minecraft).isEmpty();
    }

    static String activeBlockReason(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return "client_unavailable";
        }
        if (minecraft.screen != null) {
            return "screen_open";
        }
        if (minecraft.player.isSleeping()) {
            return "sleeping";
        }
        if (reflectedHudSuppressed(CUTSCENE_CLIENT)) {
            return "cutscene";
        }
        return "";
    }

    static boolean blocksPrompt(Minecraft minecraft) {
        return !promptBlockReason(minecraft).isEmpty();
    }

    static String promptBlockReason(Minecraft minecraft) {
        String activeReason = activeBlockReason(minecraft);
        if (!activeReason.isEmpty()) {
            return activeReason;
        }
        if (reflectedHudSuppressed(DIALOGUE_CLIENT)) {
            return "dialogue";
        }
        return "";
    }

    static boolean manualMovement(Minecraft minecraft) {
        return !manualMovementReason(minecraft).isEmpty();
    }

    static String manualMovementReason(Minecraft minecraft) {
        if (minecraft.options == null) {
            return "";
        }
        var options = minecraft.options;
        if (options.keyUp.isDown()) {
            return "forward";
        }
        if (options.keyDown.isDown()) {
            return "back";
        }
        if (options.keyLeft.isDown()) {
            return "left";
        }
        if (options.keyRight.isDown()) {
            return "right";
        }
        if (options.keyJump.isDown()) {
            return "jump";
        }
        if (options.keyShift.isDown()) {
            return "sneak";
        }
        if (options.keySprint.isDown()) {
            return "sprint";
        }
        return "";
    }

    private static boolean reflectedHudSuppressed(String className) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getMethod("hudSuppressed");
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
