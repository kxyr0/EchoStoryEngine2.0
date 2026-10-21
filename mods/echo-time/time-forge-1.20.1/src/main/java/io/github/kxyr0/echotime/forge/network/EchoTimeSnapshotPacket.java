package io.github.kxyr0.echotime.forge.network;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record EchoTimeSnapshotPacket(EchoTimeSnapshot snapshot) {
    public static void encode(EchoTimeSnapshotPacket packet, FriendlyByteBuf buffer) {
        EchoTimeSnapshot value = packet.snapshot();
        buffer.writeLong(value.instant().absoluteMicroseconds());
        buffer.writeLong(value.instant().revision().value());
        buffer.writeBoolean(value.configuredRunning());
        buffer.writeBoolean(value.paused());
        buffer.writeLong(value.effectiveRate().gameMicroseconds());
        buffer.writeLong(value.effectiveRate().simulationTicks());
        buffer.writeLong(value.serverTick());
        buffer.writeLong(value.configRevision());
    }

    public static EchoTimeSnapshotPacket decode(FriendlyByteBuf buffer) {
        EchoInstant instant = new EchoInstant(buffer.readLong(), new TimeRevision(buffer.readLong()));
        boolean running = buffer.readBoolean();
        boolean paused = buffer.readBoolean();
        TimeRate rate = new TimeRate(buffer.readLong(), buffer.readLong());
        return new EchoTimeSnapshotPacket(new EchoTimeSnapshot(instant, running, paused, rate,
                buffer.readLong(), buffer.readLong()));
    }

    public static void handle(EchoTimeSnapshotPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> EchoTimeClientPacketHandler.accept(packet.snapshot()));
        context.get().setPacketHandled(true);
    }
}
