package io.github.kxyr0.echocore.features;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FeatureNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("echo_core", "features"), () -> "1", "1"::equals, "1"::equals);
    private FeatureNetwork() { }
    public static void register() {
        CHANNEL.registerMessage(0, Snapshot.class, Snapshot::encode, Snapshot::decode, (packet, context) -> {
            context.get().enqueueWork(() -> FeatureClientState.receive(packet.player(), packet.flags()));
            context.get().setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new Snapshot(player.getUUID(), FeatureSavedData.get(player.server).state().snapshot(player.getUUID())));
    }
    public record Snapshot(UUID player, Map<String, Boolean> flags) {
        public Snapshot { flags = Map.copyOf(flags); }
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(player); buffer.writeVarInt(flags.size());
            flags.forEach((flag, value) -> { buffer.writeUtf(flag, 64); buffer.writeBoolean(value); });
        }
        public static Snapshot decode(FriendlyByteBuf buffer) {
            UUID player = buffer.readUUID(); int count = buffer.readVarInt();
            if (count < 0 || count > 256) throw new IllegalArgumentException("Invalid feature snapshot size");
            Map<String, Boolean> flags = new HashMap<>();
            for (int i = 0; i < count; i++) flags.put(FeatureState.validate(buffer.readUtf(64)), buffer.readBoolean());
            return new Snapshot(player, flags);
        }
    }
}
