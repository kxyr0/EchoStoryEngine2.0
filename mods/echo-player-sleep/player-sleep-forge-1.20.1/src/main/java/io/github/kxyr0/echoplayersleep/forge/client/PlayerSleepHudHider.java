package io.github.kxyr0.echoplayersleep.forge.client;

import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;

public final class PlayerSleepHudHider {
    private static final Set<ResourceLocation> PASSTHROUGH_OVERLAYS = Set.of(
            ResourceLocation.fromNamespaceAndPath("echo_player_sleep", "echo_player_sleep_eyes"),
            ResourceLocation.fromNamespaceAndPath("echo_dialogue", "dialogue"));
    private final ClientSleepTransitionMirror mirror;

    public PlayerSleepHudHider(ClientSleepTransitionMirror mirror) {
        this.mirror = Objects.requireNonNull(mirror, "mirror");
    }

    public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!PlayerSleepChatHider.shouldHideChat() && !mirror.shouldHideHud()) {
            return;
        }
        if (PASSTHROUGH_OVERLAYS.contains(event.getOverlay().id())) {
            return;
        }
        event.setCanceled(true);
    }
}
