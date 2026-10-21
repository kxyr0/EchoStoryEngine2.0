package io.github.kxyr0.echodialogue.forge.client.input;

import java.util.OptionalInt;
import org.lwjgl.glfw.GLFW;

/**
 * Keyboard mapping for dialogue actions.
 */
public final class ClientDialogueKeys {
    private ClientDialogueKeys() {
    }

    public static boolean isAdvance(int key) {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE;
    }

    public static OptionalInt choiceIndex(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_KP_1 -> OptionalInt.of(0);
            case GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_KP_2 -> OptionalInt.of(1);
            case GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_KP_3 -> OptionalInt.of(2);
            case GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_KP_4 -> OptionalInt.of(3);
            default -> OptionalInt.empty();
        };
    }
}
