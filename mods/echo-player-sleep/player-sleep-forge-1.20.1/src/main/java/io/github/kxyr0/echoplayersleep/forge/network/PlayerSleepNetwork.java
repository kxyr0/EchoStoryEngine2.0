package io.github.kxyr0.echoplayersleep.forge.network;

import io.github.kxyr0.echoplayersleep.api.session.PlayerSleepRevision;
import io.github.kxyr0.echoplayersleep.api.world.Ticks;
import io.github.kxyr0.echoplayersleep.forge.EchoPlayerSleepMod;
import io.github.kxyr0.echoplayersleep.forge.network.event.ClientboundEyeTransitionEvent;
import io.github.kxyr0.echoplayersleep.forge.network.message.EyeTransitionMessage;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PlayerSleepNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EchoPlayerSleepMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private PlayerSleepNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(EyeTransitionMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EyeTransitionMessage::encode)
                .decoder(EyeTransitionMessage::decode)
                .consumerMainThread(PlayerSleepNetwork::handleTransition)
                .add();
    }

    public static void closeEyes(ServerPlayer player, PlayerSleepRevision revision, Ticks duration) {
        send(player, new EyeTransitionMessage(revision, duration, true, false, false));
    }

    public static void openEyes(ServerPlayer player, PlayerSleepRevision revision,
                                Ticks duration, boolean sleepyBlink, boolean showHud) {
        send(player, new EyeTransitionMessage(revision, duration, false, sleepyBlink, showHud));
    }

    public static void openEyes(ServerPlayer player, PlayerSleepRevision revision,
                                Ticks duration, boolean sleepyBlink) {
        openEyes(player, revision, duration, sleepyBlink, true);
    }

    private static void send(ServerPlayer player, EyeTransitionMessage message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private static void handleTransition(EyeTransitionMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MinecraftForge.EVENT_BUS.post(new ClientboundEyeTransitionEvent(message)));
        context.get().setPacketHandled(true);
    }
}
