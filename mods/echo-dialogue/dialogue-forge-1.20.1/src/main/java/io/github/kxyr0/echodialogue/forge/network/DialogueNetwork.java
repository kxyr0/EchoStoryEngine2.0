package io.github.kxyr0.echodialogue.forge.network;

import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.forge.EchoDialogueMod;
import io.github.kxyr0.echodialogue.forge.api.client.DialogueCloseReason;
import io.github.kxyr0.echodialogue.forge.network.event.ClientboundDialogueCloseEvent;
import io.github.kxyr0.echodialogue.forge.network.event.ClientboundDialogueOpenEvent;
import io.github.kxyr0.echodialogue.forge.network.event.ServerboundDialogueAdvanceEvent;
import io.github.kxyr0.echodialogue.forge.network.event.ServerboundDialogueChoiceEvent;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueAdvanceMessage;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueCloseMessage;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueSelectChoiceMessage;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Versioned Forge network protocol for dialogue presentation.
 */
public final class DialogueNetwork {
    private static final String PROTOCOL = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EchoDialogueMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private DialogueNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(DialogueOpenMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialogueOpenMessage::encode)
                .decoder(DialogueOpenMessage::decode)
                .consumerMainThread(DialogueNetwork::handleOpen)
                .add();
        CHANNEL.messageBuilder(DialogueCloseMessage.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DialogueCloseMessage::encode)
                .decoder(DialogueCloseMessage::decode)
                .consumerMainThread(DialogueNetwork::handleClose)
                .add();
        CHANNEL.messageBuilder(DialogueAdvanceMessage.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueAdvanceMessage::encode)
                .decoder(DialogueAdvanceMessage::decode)
                .consumerMainThread(DialogueNetwork::handleAdvance)
                .add();
        CHANNEL.messageBuilder(DialogueSelectChoiceMessage.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueSelectChoiceMessage::encode)
                .decoder(DialogueSelectChoiceMessage::decode)
                .consumerMainThread(DialogueNetwork::handleChoice)
                .add();
    }

    public static void sendOpen(ServerPlayer player, DialoguePresentation presentation) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), DialogueOpenMessage.from(presentation));
    }

    public static void sendClose(ServerPlayer player, String sessionId, int revision, DialogueCloseReason reason) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DialogueCloseMessage(sessionId, revision, reason));
    }

    public static void requestAdvance(String sessionId, int expectedRevision) {
        CHANNEL.sendToServer(new DialogueAdvanceMessage(sessionId, expectedRevision));
    }

    public static void requestChoice(String sessionId, int expectedRevision, String choiceId) {
        CHANNEL.sendToServer(new DialogueSelectChoiceMessage(sessionId, expectedRevision, choiceId));
    }

    private static void handleOpen(DialogueOpenMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new ClientboundDialogueOpenEvent(message)));
        context.get().setPacketHandled(true);
    }

    private static void handleClose(DialogueCloseMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new ClientboundDialogueCloseEvent(message)));
        context.get().setPacketHandled(true);
    }

    private static void handleAdvance(DialogueAdvanceMessage message, Supplier<NetworkEvent.Context> context) {
        Optional.ofNullable(context.get().getSender())
                .ifPresent(sender -> context.get().enqueueWork(() -> postAdvance(message, sender)));
        context.get().setPacketHandled(true);
    }

    private static void handleChoice(DialogueSelectChoiceMessage message, Supplier<NetworkEvent.Context> context) {
        Optional.ofNullable(context.get().getSender())
                .ifPresent(sender -> context.get().enqueueWork(() -> postChoice(message, sender)));
        context.get().setPacketHandled(true);
    }

    private static void postAdvance(DialogueAdvanceMessage message, ServerPlayer sender) {
        MinecraftForge.EVENT_BUS.post(new ServerboundDialogueAdvanceEvent(message, sender));
    }

    private static void postChoice(DialogueSelectChoiceMessage message, ServerPlayer sender) {
        MinecraftForge.EVENT_BUS.post(new ServerboundDialogueChoiceEvent(message, sender));
    }
}
