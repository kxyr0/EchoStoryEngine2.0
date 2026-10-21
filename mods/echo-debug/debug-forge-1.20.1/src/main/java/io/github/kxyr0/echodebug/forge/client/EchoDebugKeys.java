package io.github.kxyr0.echodebug.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class EchoDebugKeys {
    private static final String CATEGORY = "key.categories.echodebug";
    public static final KeyMapping CHANGE_SHAPE = new KeyMapping("key.echo_debug.change_shape",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping FINALIZE_SELECTION = new KeyMapping("key.echo_debug.finalize_selection",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, CATEGORY);
    public static final KeyMapping CLEAR_NPC_TRAILS = new KeyMapping("key.echo_debug.clear_npc_trails",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    private EchoDebugKeys() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(CHANGE_SHAPE);
        event.register(FINALIZE_SELECTION);
        event.register(CLEAR_NPC_TRAILS);
    }
}
