package io.github.kxyr0.echoiteminspection.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Forge key event runs after click enqueue but before Minecraft.handleKeybinds. */
public final class WorldInspectionInput {
    public static final KeyMapping INSPECT = new KeyMapping("key.echo_item_inspection.inspect",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E,
            "key.categories.echo_item_inspection");

    private WorldInspectionInput() { }

    public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(INSPECT); }

    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && INSPECT.matches(event.getKey(), event.getScanCode())) {
            WorldInspectionRuntime.activate();
        }
    }

    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS && INSPECT.matchesMouse(event.getButton())
                && WorldInspectionRuntime.activate()) event.setCanceled(true);
    }
}
