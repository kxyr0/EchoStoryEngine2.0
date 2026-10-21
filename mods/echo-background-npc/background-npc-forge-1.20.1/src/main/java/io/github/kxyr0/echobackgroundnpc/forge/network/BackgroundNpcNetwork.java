package io.github.kxyr0.echobackgroundnpc.forge.network;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientLine;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import io.github.kxyr0.echobackgroundnpc.forge.entity.BackgroundNpcEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import io.github.kxyr0.echobackgroundnpc.core.render.MovementSnapshot;

/** Change-based client channel used only when the selected ambient line changes. */
public final class BackgroundNpcNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("echo_background_npc", "main"), () -> PROTOCOL,
            PROTOCOL::equals, PROTOCOL::equals);

    private BackgroundNpcNetwork() { }

    public static void register() {
        CHANNEL.messageBuilder(AmbientOverlayMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AmbientOverlayMessage::encode).decoder(AmbientOverlayMessage::decode)
                .consumerMainThread(BackgroundNpcNetwork::handle).add();
        CHANNEL.messageBuilder(MovementSnapshotMessage.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MovementSnapshotMessage::encode).decoder(MovementSnapshotMessage::decode)
                .consumerMainThread(BackgroundNpcNetwork::handleMovement).add();
    }

    public static void show(ServerPlayer player, String id, String dialogueId, AmbientLine line,
            long durationMillis, BackgroundNpcEntity speaker) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AmbientOverlayMessage(id, dialogueId, line.speaker(), line.text(),
                        line.voiceStem().orElse(""), speaker.level().dimension().location().toString(),
                        speaker.getX(), speaker.getY(), speaker.getZ(), durationMillis, true));
    }

    public static void clear(ServerPlayer player, String id) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), AmbientOverlayMessage.clear(id));
    }

    public static void movement(BackgroundNpcEntity entity, MovementSnapshot snapshot) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                MovementSnapshotMessage.from(entity, snapshot));
    }

    private static void handle(AmbientOverlayMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(message::present);
        context.get().setPacketHandled(true);
    }
    private static void handleMovement(MovementSnapshotMessage message,
            Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(message::apply);
        context.get().setPacketHandled(true);
    }
}
