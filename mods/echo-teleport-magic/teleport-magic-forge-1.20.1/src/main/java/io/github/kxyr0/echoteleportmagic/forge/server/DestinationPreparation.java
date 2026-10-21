package io.github.kxyr0.echoteleportmagic.forge.server;

import io.github.kxyr0.echoteleportmagic.api.TeleportExecution;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.AABB;

public final class DestinationPreparation {
    private static final TicketType<UUID> TICKET = TicketType.create("echo_teleport_magic", UUID::compareTo, 200);
    private final Map<UUID, Prepared> tickets = new HashMap<>();
    private final MinecraftServer server;
    public DestinationPreparation(MinecraftServer server) { this.server = server; }

    public void prepare(TeleportExecution e) {
        var subject = TeleportSubjects.require(server, e.subject());
        if (!subject.isAlive() || subject.isRemoved()) throw new IllegalArgumentException("Subject unavailable");
        if (subject.isSleeping() || subject.isVehicle()) throw new IllegalArgumentException("Sleeping or passenger-carrying subject unsupported");
        if (e.preview()) return;
        var d = e.destination();
        ServerLevel level = (ServerLevel) subject.level();
        if (!level.dimension().location().toString().equals(d.dimension())) {
            throw new IllegalArgumentException("TELEPORT_CROSS_DIMENSION_UNSUPPORTED");
        }
        BlockPos pos = BlockPos.containing(d.x(), d.y(), d.z());
        if (!level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)
                || d.y() + subject.getBbHeight() >= level.getMaxBuildHeight()) {
            throw new IllegalArgumentException("Destination outside world bounds");
        }
        ChunkPos chunk = new ChunkPos(pos);
        level.getChunkSource().addRegionTicket(TICKET, chunk, 2, e.id().value());
        tickets.put(e.id().value(), new Prepared(level, chunk));
        try {
            level.getChunkSource().getChunk(chunk.x, chunk.z, ChunkStatus.FULL, true);
            AABB bounds = subject.getBoundingBox().move(d.x() - subject.getX(), d.y() - subject.getY(), d.z() - subject.getZ());
            if (!level.noCollision(subject, bounds)) throw new IllegalArgumentException("Destination obstructed");
        } catch (RuntimeException failure) { release(e); throw failure; }
    }
    public void release(TeleportExecution e) {
        Prepared prepared = tickets.remove(e.id().value());
        if (prepared != null) prepared.level().getChunkSource()
                .removeRegionTicket(TICKET, prepared.chunk(), 2, e.id().value());
    }
    private record Prepared(ServerLevel level, ChunkPos chunk) { }
}
