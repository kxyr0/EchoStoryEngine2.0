package io.github.kxyr0.echomagic.forge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class MagicKeyMappings {
    public static final KeyMapping DRAW_GESTURE = new KeyMapping("key.echo_magic.draw_gesture",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.echo_magic");
    public static final KeyMapping SELECT_SCHOOL = new KeyMapping("key.echo_magic.select_school",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.echo_magic");
    public static final KeyMapping RETRY_TRAINING_SAMPLE = new KeyMapping("key.echo_magic.retry_training",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.echo_magic");
    private MagicKeyMappings() { }
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(DRAW_GESTURE);
        event.register(SELECT_SCHOOL);
        event.register(RETRY_TRAINING_SAMPLE);
    }
}
