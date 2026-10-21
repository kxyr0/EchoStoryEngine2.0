package io.github.kxyr0.echoobservation.authoring.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ObservationAuthoringKeys {
    private static final String CATEGORY = "key.categories.echo_observation_authoring";
    public static final KeyMapping TOGGLE = key("toggle", GLFW.GLFW_KEY_F6);
    public static final KeyMapping PLACE = key("place", GLFW.GLFW_KEY_INSERT);
    public static final KeyMapping PREVIOUS = key("previous", GLFW.GLFW_KEY_LEFT_BRACKET);
    public static final KeyMapping NEXT = key("next", GLFW.GLFW_KEY_RIGHT_BRACKET);
    public static final KeyMapping CLEAR = key("clear", GLFW.GLFW_KEY_DELETE);
    public static final KeyMapping PICKER = key("picker", GLFW.GLFW_KEY_TAB);

    private ObservationAuthoringKeys() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
        event.register(PLACE);
        event.register(PREVIOUS);
        event.register(NEXT);
        event.register(CLEAR);
        event.register(PICKER);
    }

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.echo_observation_authoring." + name,
                InputConstants.Type.KEYSYM, code, CATEGORY);
    }
}
