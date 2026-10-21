package io.github.kxyr0.echodialogue.forge.client;

import com.mojang.blaze3d.platform.Window;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.EchoDialogueMod;
import io.github.kxyr0.echodialogue.forge.client.input.ClientDialogueChoiceScreenController;
import io.github.kxyr0.echodialogue.forge.client.input.ClientDialogueKeys;
import io.github.kxyr0.echodialogue.forge.client.lifecycle.ClientDialogueLifecycle;
import io.github.kxyr0.echodialogue.forge.client.reload.DialogueVisualReloadListener;
import io.github.kxyr0.echodialogue.forge.client.state.ClientDialogueState;
import io.github.kxyr0.echodialogue.forge.network.DialogueNetwork;
import io.github.kxyr0.echodialogue.forge.network.event.ClientboundDialogueCloseEvent;
import io.github.kxyr0.echodialogue.forge.network.event.ClientboundDialogueOpenEvent;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only overlay, packet event receiver and bounded input bridge.
 */
public final class ClientDialogueClient {
    private static final ClientDialogueState STATE = new ClientDialogueState();
    private static final ClientDialogueAnimationState ANIMATION = new ClientDialogueAnimationState();
    private static final ClientDialogueChoiceScreenController CHOICE_SCREEN =
            new ClientDialogueChoiceScreenController(
                    ClientDialogueClient::choiceScreenActive,
                    ClientDialogueClient::requestChoiceAt,
                    ClientDialogueClient::requestMouseChoice,
                    ClientDialogueClient::renderDialogue);

    private ClientDialogueClient() {
    }

    public static boolean hudSuppressed() {
        return STATE.current().isPresent() || ANIMATION.renderState(nowMs()).isPresent();
    }

    public static void clearVisualCache() {
        ClientDialogueRenderer.clearVisualCache();
    }

    @Mod.EventBusSubscriber(modid = EchoDialogueMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("dialogue", ClientDialogueClient::render);
        }

        @SubscribeEvent
        public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new DialogueVisualReloadListener());
        }
    }

    @Mod.EventBusSubscriber(modid = EchoDialogueMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {
        }

        @SubscribeEvent
        public static void open(ClientboundDialogueOpenEvent event) {
            Optional<DialogueOpenMessage> previous = STATE.current();
            if (STATE.open(event.message())) {
                ClientDialogueLifecycle.transition(previous, event.message());
                ANIMATION.open(event.message(), nowMs());
                CHOICE_SCREEN.sync();
            }
        }

        @SubscribeEvent
        public static void close(ClientboundDialogueCloseEvent event) {
            Optional<DialogueOpenMessage> previous = STATE.current();
            if (STATE.close(event.message().sessionId(), event.message().revision())) {
                previous.ifPresent(message -> ClientDialogueLifecycle.close(message, event.message().reason()));
                ANIMATION.close(nowMs());
                CHOICE_SCREEN.sync();
            }
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientDialogueLifecycle.disconnect(STATE.current());
            STATE.clear();
            ANIMATION.clear();
            ClientDialogueRenderer.clearVisualCache();
            CHOICE_SCREEN.close();
        }

        @SubscribeEvent
        public static void key(InputEvent.Key event) {
            if (event.getAction() != GLFW.GLFW_PRESS || CHOICE_SCREEN.isOpen()) {
                return;
            }
            OptionalInt choiceIndex = ClientDialogueKeys.choiceIndex(event.getKey());
            if (choiceIndex.isPresent()) {
                requestChoiceAt(choiceIndex.orElseThrow());
                return;
            }
            if (ClientDialogueKeys.isAdvance(event.getKey())) {
                if (!revealText()) {
                    requestAdvance();
                }
            }
        }

        @SubscribeEvent
        public static void mouse(InputEvent.MouseButton.Pre event) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == GLFW.GLFW_PRESS
                    && Minecraft.getInstance().screen == null && STATE.current().isPresent()) {
                if (!requestChoice() && !revealText()) {
                    requestAdvance();
                }
                event.setCanceled(true);
            }
        }
    }

    private static boolean requestAdvance() {
        if (!CHOICE_SCREEN.dialogueInputAllowed()) {
            return false;
        }
        Optional<DialogueOpenMessage> request = STATE.requestAdvance();
        request.ifPresent(message -> DialogueNetwork.requestAdvance(message.sessionId(), message.revision()));
        return request.isPresent();
    }

    private static boolean revealText() {
        Optional<DialogueOpenMessage> current = STATE.current()
                .filter(message -> message.kind() == DialoguePresentationKind.LINE);
        if (current.isEmpty() || !ANIMATION.revealText(nowMs())) {
            return false;
        }
        ClientDialogueLifecycle.reveal(current.orElseThrow());
        return true;
    }

    private static boolean requestChoiceAt(int index) {
        if (!CHOICE_SCREEN.dialogueInputAllowed()) {
            return false;
        }
        return sendChoice(STATE.requestChoiceAt(index));
    }

    private static boolean requestChoice() {
        if (!CHOICE_SCREEN.dialogueInputAllowed()) {
            return false;
        }
        return sendChoice(mouseChoice().flatMap(STATE::requestChoice));
    }

    private static boolean requestMouseChoice(double mouseX, double mouseY) {
        return sendChoice(ClientDialogueRenderer.choiceAt(mouseX, mouseY).flatMap(STATE::requestChoice));
    }

    private static boolean sendChoice(Optional<ClientDialogueState.ChoiceRequest> request) {
        request.ifPresent(choiceRequest ->
                DialogueNetwork.requestChoice(
                        choiceRequest.sessionId(), choiceRequest.revision(), choiceRequest.choiceId()));
        return request.isPresent();
    }

    private static Optional<String> mouseChoice() {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        double x = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double y = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        return ClientDialogueRenderer.choiceAt(x, y);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        CHOICE_SCREEN.sync();
        if (CHOICE_SCREEN.isOpen()) {
            return;
        }
        renderDialogue(graphics, width, height);
    }

    private static void renderDialogue(GuiGraphics graphics, int width, int height) {
        ClientDialogueRenderer.render(graphics, ANIMATION, width, height, nowMs());
    }

    private static boolean choiceScreenActive() {
        return STATE.current()
                .map(message -> message.kind() == DialoguePresentationKind.CHOICE)
                .orElse(false);
    }

    private static long nowMs() {
        return net.minecraft.Util.getMillis();
    }

}
