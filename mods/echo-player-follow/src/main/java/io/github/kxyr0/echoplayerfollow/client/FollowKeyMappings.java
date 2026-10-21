package io.github.kxyr0.echoplayerfollow.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class FollowKeyMappings {
    private static final String CATEGORY = "key.categories.echo_player_follow";
    public static final KeyMapping FOLLOW = new KeyMapping("key.echo_player_follow.follow_npc",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private FollowKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(FOLLOW);
    }
}
