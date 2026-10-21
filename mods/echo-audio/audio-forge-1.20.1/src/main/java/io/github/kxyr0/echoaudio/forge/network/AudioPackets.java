package io.github.kxyr0.echoaudio.forge.network;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.forge.client.AudioClient;
import io.github.kxyr0.echoaudio.forge.server.AudioServer;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class AudioPackets {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("echo_audio", "playback"), () -> "1", "1"::equals, "1"::equals);
    private AudioPackets() { }
    public static void register() {
        CHANNEL.registerMessage(0, AudioIntent.class, AudioPackets::write, AudioPackets::read,
                AudioPackets::start, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Stop.class, (p, b) -> { b.writeUUID(p.id().value()); b.writeLong(p.fade()); },
                b -> new Stop(new AudioPlaybackId(b.readUUID()), b.readLong()), AudioPackets::stop,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, End.class, (p, b) -> { b.writeUUID(p.id().value()); b.writeUtf(p.detail(), 1024); },
                b -> new End(new AudioPlaybackId(b.readUUID()), b.readUtf(1024)), AudioPackets::end,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void send(ServerPlayer player, AudioIntent intent) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), intent);
    }
    public static void stop(ServerPlayer player, AudioPlaybackId id, long fade) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Stop(id, fade));
    }
    public static void ended(AudioPlaybackId id, String detail) { CHANNEL.sendToServer(new End(id, detail)); }
    private static void write(AudioIntent intent, FriendlyByteBuf buf) {
        buf.writeUUID(intent.id().value()); buf.writeUtf(intent.owner(), 512);
        buf.writeMap(intent.cue().payload(), (b, v) -> b.writeUtf(v, 128), (b, v) -> b.writeUtf(v, 512));
        buf.writeUtf(intent.dimension(), 256); buf.writeDouble(intent.x()); buf.writeDouble(intent.y()); buf.writeDouble(intent.z());
    }
    private static AudioIntent read(FriendlyByteBuf buf) {
        var id = new AudioPlaybackId(buf.readUUID()); String owner = buf.readUtf(512);
        Map<String, String> map = buf.readMap(b -> b.readUtf(128), b -> b.readUtf(512));
        return new AudioIntent(id, owner, AudioCue.decode(map), buf.readUtf(256), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    private static void start(AudioIntent intent, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> AudioClient.instance().play(intent)));
        ctx.get().setPacketHandled(true);
    }
    private static void stop(Stop packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> AudioClient.instance().stop(packet.id(), packet.fade())));
        ctx.get().setPacketHandled(true);
    }
    private static void end(End packet, Supplier<NetworkEvent.Context> ctx) {
        var sender = ctx.get().getSender();
        ctx.get().enqueueWork(() -> { if (sender != null) AudioServer.of(sender.getServer()).ended(sender, packet.id(), packet.detail()); });
        ctx.get().setPacketHandled(true);
    }
    public record Stop(AudioPlaybackId id, long fade) { }
    public record End(AudioPlaybackId id, String detail) { }
}
