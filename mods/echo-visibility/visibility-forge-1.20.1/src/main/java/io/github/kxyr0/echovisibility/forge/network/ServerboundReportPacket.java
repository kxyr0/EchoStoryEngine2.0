package io.github.kxyr0.echovisibility.forge.network;

import io.github.kxyr0.echovisibility.api.report.VisibilityReport;
import io.github.kxyr0.echovisibility.forge.network.codec.VisibilityReportCodec;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundReportPacket(VisibilityReport report) {
    public void encode(FriendlyByteBuf buffer) {
        VisibilityReportCodec.write(buffer, report);
    }

    public static ServerboundReportPacket decode(FriendlyByteBuf buffer) {
        return new ServerboundReportPacket(VisibilityReportCodec.read(buffer));
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> post(context.getSender()));
        context.setPacketHandled(true);
    }

    private void post(ServerPlayer player) {
        if (player != null) {
            MinecraftForge.EVENT_BUS.post(new VisibilityReportNetworkEvent(player, report));
        }
    }
}
