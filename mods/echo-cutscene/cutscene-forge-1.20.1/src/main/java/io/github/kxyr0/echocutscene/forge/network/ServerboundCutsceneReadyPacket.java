package io.github.kxyr0.echocutscene.forge.network;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundCutsceneReadyPacket(CutscenePlaybackId playbackId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(playbackId.value());
    }

    public static ServerboundCutsceneReadyPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundCutsceneReadyPacket(new CutscenePlaybackId(buffer.readUtf()));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> markReady(context.getSender()));
        context.setPacketHandled(true);
    }

    private void markReady(ServerPlayer player) {
        if (player != null && EchoCutsceneNetwork.host() != null) {
            EchoCutsceneNetwork.host().markPrepared(player, playbackId);
        }
    }
}
