package io.github.kxyr0.echoconcentration.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ConcentrationKeyMappings {
    private static final String CATEGORY = "key.categories.echo_concentration";
    public static final KeyMapping TOGGLE = new KeyMapping("key.echo_concentration.toggle",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

    private ConcentrationKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    public static boolean matches(int keyCode, int scanCode) {
        return TOGGLE.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
    }
}
