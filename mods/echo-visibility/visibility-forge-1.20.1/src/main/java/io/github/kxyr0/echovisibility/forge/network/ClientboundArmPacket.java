package io.github.kxyr0.echovisibility.forge.network;

import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.target.VisibilityTargetBounds;
import io.github.kxyr0.echovisibility.forge.client.runtime.VisibilityClientRuntime;
import io.github.kxyr0.echovisibility.forge.network.codec.VisibilityDefinitionCodec;
import io.github.kxyr0.echovisibility.forge.network.codec.VisibilityGeometryCodec;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ClientboundArmPacket(
        VisibilityWatchDefinition definition,
        VisibilityWatchGeneration generation,
        long connectionGeneration,
        Optional<VisibilityTargetBounds> stableBounds,
        int clientEntityId) {
    public void encode(FriendlyByteBuf buffer) {
        VisibilityDefinitionCodec.write(buffer, definition);
        buffer.writeLong(generation.value());
        buffer.writeLong(connectionGeneration);
        buffer.writeBoolean(stableBounds.isPresent());
        stableBounds.ifPresent(value -> VisibilityGeometryCodec.writeBounds(buffer, value));
        buffer.writeInt(clientEntityId);
    }

    public static ClientboundArmPacket decode(FriendlyByteBuf buffer) {
        VisibilityWatchDefinition definition = VisibilityDefinitionCodec.read(buffer);
        VisibilityWatchGeneration generation = new VisibilityWatchGeneration(buffer.readLong());
        long connection = buffer.readLong();
        Optional<VisibilityTargetBounds> bounds = buffer.readBoolean()
                ? Optional.of(VisibilityGeometryCodec.readBounds(buffer)) : Optional.empty();
        return new ClientboundArmPacket(definition, generation, connection, bounds, buffer.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> VisibilityClientRuntime.instance().arm(this)));
        context.setPacketHandled(true);
    }
}
