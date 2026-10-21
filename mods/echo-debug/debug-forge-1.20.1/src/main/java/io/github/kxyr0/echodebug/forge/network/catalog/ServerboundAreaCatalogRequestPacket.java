package io.github.kxyr0.echodebug.forge.network.catalog;

import io.github.kxyr0.echodebug.core.diagnostic.DebugOperationResult;
import io.github.kxyr0.echodebug.forge.network.EchoDebugNetwork;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandUseGuard;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ServerboundAreaCatalogRequestPacket() {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(true);
    }

    public static ServerboundAreaCatalogRequestPacket decode(FriendlyByteBuf buffer) {
        buffer.readBoolean();
        return new ServerboundAreaCatalogRequestPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> sendSnapshot(context.getSender()));
        context.setPacketHandled(true);
    }

    private static void sendSnapshot(ServerPlayer player) {
        if (player == null || !TriggerWandUseGuard.held(player)) {
            return;
        }
        var session = EchoDebugNetwork.sessions().session(player);
        EchoDebugNetwork.sendSelection(player, DebugOperationResult.success(session.snapshot()), false);
        EchoDebugNetwork.sendCatalog(player);
    }
}
