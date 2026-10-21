package io.github.kxyr0.echothoughtspace.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ThoughtSpaceKeys {
    private static final String CATEGORY = "key.categories.echo_thought_space";
    public static final KeyMapping TOGGLE = new KeyMapping("key.echo_thought_space.toggle",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY);

    private ThoughtSpaceKeys() { }
    public static void register(RegisterKeyMappingsEvent event) { event.register(TOGGLE); }
}

