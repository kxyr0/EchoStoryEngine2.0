package io.github.kxyr0.echocutscene.forge.network;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundCutsceneSkipPacket(String requestId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(requestId);
    }

    public static ServerboundCutsceneSkipPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundCutsceneSkipPacket(buffer.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> stop(context.getSender()));
        context.setPacketHandled(true);
    }

    private void stop(ServerPlayer player) {
        if (player != null && EchoCutsceneNetwork.host() != null) {
            EchoCutsceneNetwork.host().stopActive(player, new CutsceneCommandId(requestId));
        }
    }
}
