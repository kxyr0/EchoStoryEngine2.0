package io.github.kxyr0.echocutscene.forge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class ClientInputSuppressor {
    private final ClientCameraPlayback playback;
    private final ClientAuthoringSession authoring;

    ClientInputSuppressor(ClientCameraPlayback playback, ClientAuthoringSession authoring) {
        this.playback = playback;
        this.authoring = authoring;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (authoringKeysAllowed()) {
            while (EchoCutsceneClient.CUTSCENE_RECORD.consumeClick()) {
                authoring.toggleRecording();
            }
            while (EchoCutsceneClient.CUTSCENE_PITCH_FLIGHT.consumeClick()) {
                authoring.togglePitchFlight();
            }
            while (EchoCutsceneClient.CUTSCENE_SET_TARGET.consumeClick()) {
                authoring.setFixedTargetFromView();
            }
            while (EchoCutsceneClient.CUTSCENE_CAMERA_MODE.consumeClick()) {
                authoring.cycleCameraMode();
            }
        }
        if (blocksGameplayInput()) {
            clearGameplayMappings(Minecraft.getInstance().options);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            playback.tick();
            authoring.renderTick();
        }
    }

    @SubscribeEvent
    public void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (blocksGameplayInput()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (blocksGameplayInput()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (blocksGameplayInput()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private boolean blocksGameplayInput() {
        Minecraft minecraft = Minecraft.getInstance();
        return (playback.playing() || authoring.active()) && minecraft.screen == null;
    }

    private boolean authoringKeysAllowed() {
        return authoring.active() && Minecraft.getInstance().screen == null;
    }

    private static void clearGameplayMappings(Options options) {
        clear(options.keyUp, options.keyLeft, options.keyDown, options.keyRight, options.keyJump,
                options.keyShift, options.keySprint, options.keyInventory, options.keySwapOffhand,
                options.keyDrop, options.keyUse, options.keyAttack, options.keySaveHotbarActivator,
                options.keyLoadHotbarActivator, options.keyPickItem);
        for (KeyMapping mapping : options.keyHotbarSlots) {
            clear(mapping);
        }
    }

    private static void clear(KeyMapping... mappings) {
        for (KeyMapping mapping : mappings) {
            mapping.setDown(false);
            while (mapping.consumeClick()) {
                // Drain queued clicks created before the playback tick ran.
            }
        }
    }
}
