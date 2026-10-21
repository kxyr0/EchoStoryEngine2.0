package io.github.kxyr0.echodialogue.forge.client;

import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.EchoDialogueMod;
import io.github.kxyr0.echodialogue.forge.api.client.EchoDialogueTransientLineEvent;
import io.github.kxyr0.echodialogue.forge.api.client.overlay.EchoDialogueTransientClearEvent;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Standard Dialogue renderer hosted without a server dialogue session. */
public final class ClientTransientDialogueOverlay {
    private static final Runtime RUNTIME = new Runtime();

    private ClientTransientDialogueOverlay() { }

    @Mod.EventBusSubscriber(modid = EchoDialogueMod.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        private ModBusEvents() { }

        @SubscribeEvent
        public static void register(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("transient_dialogue", ClientTransientDialogueOverlay::render);
        }
    }

    @Mod.EventBusSubscriber(modid = EchoDialogueMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() { }

        @SubscribeEvent public static void show(EchoDialogueTransientLineEvent event) { RUNTIME.open(event); }
        @SubscribeEvent public static void clear(EchoDialogueTransientClearEvent event) { RUNTIME.clear(event); }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { RUNTIME.clear(); }
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        RUNTIME.render(graphics, width, height, ClientDialogueClient.hudSuppressed());
    }

    private static final class Runtime {
        private final ClientDialogueAnimationState animation = new ClientDialogueAnimationState();
        private long expiresAtMillis;
        private boolean closing;
        private int revision;
        private String presentationId = "";

        void open(EchoDialogueTransientLineEvent event) {
            long now = nowMs();
            DialogueOpenMessage message = new DialogueOpenMessage(
                    "transient/" + event.presentationId(), ++revision, Optional.empty(),
                    DialoguePresentationKind.LINE, event.speakerDisplayName(), Optional.empty(), Optional.empty(),
                    event.text(), true, List.of(), List.of(), false, false, "echo_dialogue:transient");
            animation.open(message, now);
            presentationId = event.presentationId();
            expiresAtMillis = now + event.durationMillis();
            closing = false;
        }

        void render(GuiGraphics graphics, int width, int height, boolean regularDialogueVisible) {
            long now = nowMs();
            if (!closing && expiresAtMillis > 0L && now >= expiresAtMillis) {
                animation.close(now);
                closing = true;
            }
            if (regularDialogueVisible) {
                animation.renderState(now);
            } else {
                ClientDialogueRenderer.render(graphics, animation, width, height, now);
            }
        }

        void clear() {
            animation.clear();
            expiresAtMillis = 0L;
            closing = false;
            presentationId = "";
        }

        void clear(EchoDialogueTransientClearEvent event) {
            if (presentationId.equals(event.presentationId())) {
                clear();
            }
        }
    }

    private static long nowMs() { return net.minecraft.Util.getMillis(); }
}
