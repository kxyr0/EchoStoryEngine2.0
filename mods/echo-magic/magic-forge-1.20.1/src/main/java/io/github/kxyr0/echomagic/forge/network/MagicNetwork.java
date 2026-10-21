package io.github.kxyr0.echomagic.forge.network;

import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntity;
import io.github.kxyr0.echomagic.core.gesture.MagicGestureMode;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicDrawingStatePacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicSchoolSelectedPacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTemplatesPacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTrainingSamplePacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTrainingStatusPacket;
import io.github.kxyr0.echomagic.forge.network.gesture.MagicGestureTrainingUndoPacket;
import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class MagicNetwork {
    private static final String PROTOCOL = "9";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EchoMagicMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private MagicNetwork() { }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(MagicCastIntentPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MagicCastIntentPacket::encode).decoder(MagicCastIntentPacket::decode)
                .consumerMainThread(MagicCastIntentPacket::handle).add();
        CHANNEL.messageBuilder(MagicDrawingStatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MagicDrawingStatePacket::encode).decoder(MagicDrawingStatePacket::decode)
                .consumerMainThread(MagicDrawingStatePacket::handle).add();
        CHANNEL.messageBuilder(MagicSchoolSelectedPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MagicSchoolSelectedPacket::encode).decoder(MagicSchoolSelectedPacket::decode)
                .consumerMainThread(MagicSchoolSelectedPacket::handle).add();
        CHANNEL.messageBuilder(MagicCastAcceptedPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MagicCastAcceptedPacket::encode).decoder(MagicCastAcceptedPacket::decode)
                .consumerMainThread(MagicCastAcceptedPacket::handle).add();
        CHANNEL.messageBuilder(MagicCastRejectedPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MagicCastRejectedPacket::encode).decoder(MagicCastRejectedPacket::decode)
                .consumerMainThread(MagicCastRejectedPacket::handle).add();
        CHANNEL.messageBuilder(MagicPlayerStateSnapshotPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MagicPlayerStateSnapshotPacket::encode).decoder(MagicPlayerStateSnapshotPacket::decode)
                .consumerMainThread(MagicPlayerStateSnapshotPacket::handle).add();
        CHANNEL.messageBuilder(MagicImpactPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MagicImpactPacket::encode).decoder(MagicImpactPacket::decode)
                .consumerMainThread(MagicImpactPacket::handle).add();
        CHANNEL.messageBuilder(MagicGestureTrainingSamplePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MagicGestureTrainingSamplePacket::encode).decoder(MagicGestureTrainingSamplePacket::decode)
                .consumerMainThread(MagicGestureTrainingSamplePacket::handle).add();
        CHANNEL.messageBuilder(MagicGestureTrainingStatusPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MagicGestureTrainingStatusPacket::encode).decoder(MagicGestureTrainingStatusPacket::decode)
                .consumerMainThread(MagicGestureTrainingStatusPacket::handle).add();
        CHANNEL.messageBuilder(MagicGestureTrainingUndoPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MagicGestureTrainingUndoPacket::encode).decoder(MagicGestureTrainingUndoPacket::decode)
                .consumerMainThread(MagicGestureTrainingUndoPacket::handle).add();
        CHANNEL.messageBuilder(MagicGestureTemplatesPacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MagicGestureTemplatesPacket::encode).decoder(MagicGestureTemplatesPacket::decode)
                .consumerMainThread(MagicGestureTemplatesPacket::handle).add();
    }

    public static void sendGesture(MagicGestureMode mode, List<List<MagicGesturePoint>> strokes) {
        CHANNEL.sendToServer(new MagicCastIntentPacket(mode, strokes));
    }
    public static void sendDrawingState(boolean active) {
        CHANNEL.sendToServer(new MagicDrawingStatePacket(active));
    }
    public static void sendTrainingSample(MagicGestureTemplateTarget target, List<MagicGesturePoint> points) {
        CHANNEL.sendToServer(new MagicGestureTrainingSamplePacket(target, points));
    }
    public static void undoTrainingSample(MagicGestureTemplateTarget target) {
        CHANNEL.sendToServer(new MagicGestureTrainingUndoPacket(target));
    }
    public static void send(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    public static void sendTracking(MagicProjectileEntity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
}
