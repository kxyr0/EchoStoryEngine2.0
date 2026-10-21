package io.github.kxyr0.echodamagenumbers.network;

import io.github.kxyr0.echodamagenumbers.EchoDamageNumbersMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DamageNumberNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(EchoDamageNumbersMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private DamageNumberNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(DamagePopupPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DamagePopupPacket::encode)
                .decoder(DamagePopupPacket::decode)
                .consumerMainThread(DamagePopupPacket::handle)
                .add();
    }

    public static void send(ServerPlayer player, DamagePopupPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}

