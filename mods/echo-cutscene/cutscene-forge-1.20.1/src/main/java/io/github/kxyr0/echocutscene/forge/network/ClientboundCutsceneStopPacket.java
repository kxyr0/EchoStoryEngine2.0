package io.github.kxyr0.echocutscene.forge.network;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import io.github.kxyr0.echocutscene.forge.client.EchoCutsceneClient;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundCutsceneStopPacket(CutscenePlaybackId playbackId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(playbackId.value());
    }

    public static ClientboundCutsceneStopPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundCutsceneStopPacket(new CutscenePlaybackId(buffer.readUtf()));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoCutsceneClient.end(playbackId)));
        context.setPacketHandled(true);
    }
}
