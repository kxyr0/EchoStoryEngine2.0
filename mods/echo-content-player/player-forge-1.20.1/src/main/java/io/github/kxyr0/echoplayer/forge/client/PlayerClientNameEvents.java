package io.github.kxyr0.echoplayer.forge.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Client nameplate bridge backed by the latest server profile mirror.
 */
public final class PlayerClientNameEvents {
    private PlayerClientNameEvents() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(PlayerClientNameEvents::onRenderNameTag);
    }

    private static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerClientMirror.INSTANCE.resolvedName(player.getUUID())
                    .ifPresent(name -> event.setContent(Component.literal(name)));
        }
    }
}
