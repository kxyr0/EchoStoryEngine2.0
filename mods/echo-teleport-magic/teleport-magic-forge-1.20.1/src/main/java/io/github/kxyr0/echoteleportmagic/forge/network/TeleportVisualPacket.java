package io.github.kxyr0.echoteleportmagic.forge.network;

import io.github.kxyr0.echoteleportmagic.api.TeleportAnchor;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record TeleportVisualPacket(UUID execution, UUID subject, TeleportAnchor anchor,
        long startTick, long seed, Kind kind, boolean preview, boolean arrival, double freeze) {
    public enum Kind { START, COMMIT, CANCEL, COMPLETE, DEBUG }

    public void encode(FriendlyByteBuf out) {
        out.writeUUID(execution); out.writeUUID(subject);
        out.writeDouble(anchor.x()); out.writeDouble(anchor.y()); out.writeDouble(anchor.z());
        out.writeFloat(anchor.yaw()); out.writeFloat(anchor.width()); out.writeFloat(anchor.height());
        out.writeLong(startTick); out.writeLong(seed); out.writeEnum(kind);
        out.writeBoolean(preview); out.writeBoolean(arrival); out.writeDouble(freeze);
    }
    public static TeleportVisualPacket decode(FriendlyByteBuf in) {
        UUID execution = in.readUUID(), subject = in.readUUID();
        TeleportAnchor anchor = new TeleportAnchor(in.readDouble(), in.readDouble(), in.readDouble(),
                in.readFloat(), in.readFloat(), in.readFloat());
        long tick = in.readLong(), seed = in.readLong();
        Kind kind = in.readEnum(Kind.class);
        boolean preview = in.readBoolean(), arrival = in.readBoolean();
        double freeze = in.readDouble();
        if (!Double.isFinite(freeze) || freeze < -2 || freeze > 4) throw new IllegalArgumentException("Visual time");
        return new TeleportVisualPacket(execution, subject, anchor, tick, seed, kind, preview, arrival, freeze);
    }
}
