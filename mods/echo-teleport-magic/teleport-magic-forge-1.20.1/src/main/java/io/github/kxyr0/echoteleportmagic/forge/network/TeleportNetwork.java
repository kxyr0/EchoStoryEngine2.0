package io.github.kxyr0.echoteleportmagic.forge.network;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TeleportNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("echo_teleport_magic", "visual"), () -> "1", "1"::equals, "1"::equals);
    private TeleportNetwork() { }

    public static void register() {
        CHANNEL.registerMessage(0, TeleportVisualPacket.class, TeleportVisualPacket::encode,
                TeleportVisualPacket::decode, (packet, supplier) -> {
                    var context = supplier.get();
                    context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> io.github.kxyr0.echoteleportmagic.forge.client.TeleportClient.receive(packet)));
                    context.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(ServerPlayer player, TeleportVisualPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
